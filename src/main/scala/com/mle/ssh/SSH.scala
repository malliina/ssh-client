package com.mle.ssh

import java.io.{Closeable, FileInputStream, InputStream, OutputStream}
import java.nio.file.{Files, Path}
import java.util.concurrent.Executors

import com.jcraft.jsch._
import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.ssh.Creds.{KeyCreds, PassCreds}
import com.mle.storage.{StorageLong, StorageSize}
import com.mle.streams.{ReplayRxOutputStream, RxOutputStream}
import com.mle.util.Utils
import rx.lang.scala.subjects.{BehaviorSubject, ReplaySubject}
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
 * @author Michael
 */
class SSH(host: String, port: Int, user: String, creds: Creds, strictKeyChecking: Boolean = false) extends Closeable {

  def this(host: String, port: Int, user: String, key: Path) = this(host, port, user, KeyCreds(key))

  def this(host: String, port: Int, user: String, pass: String) = this(host, port, user, PassCreds(pass))

  val EXEC = "exec"
  val SHELL = "shell"

  private val executor = Executors.newCachedThreadPool()
  private implicit val ec = ExecutionContext.fromExecutor(executor)

  val session = initSession()

  def describe = s"$user@$host:$port"

  /**
   * SCPs local `file` to remote path `dest`.
   *
   * @see http://www.jcraft.com/jsch/examples/ScpTo.java.html
   *
   * @param file local file
   * @param dest remote destination
   * @return the number of bytes transferred in total
   */
  def scpAwait(file: Path, dest: String, timeout: Duration = 10.hours): Try[StorageSize] =
    Try {
      scp(file, dest).toBlocking.last
    }

  /**
   *
   * @param file source
   * @param dest destination
   * @return progress indicating the total number of bytes sent
   */
  def scp(file: Path, dest: String): Observable[StorageSize] = {
    val subject = BehaviorSubject[StorageSize](StorageSize.empty)
    Future {
      withChannel(openExecChannel(session, s"scp -t $dest"))(channel => {
        Utils.using(channel.getOutputStream)(out => {
          val in = channel.getInputStream

          def connect() = withErrorCheck {
            channel.connect()
          }
          def sendLastMod() = withErrorCheck {
            val lastMod = Files.getLastModifiedTime(file).toMillis / 1000
            val lastModCommand = s"T $lastMod 0 $lastMod 0"
            writeLine(out, lastModCommand)
          }
          def sendSize() = withErrorCheck {
            val size = Files.size(file)
            val sizeCommand = s"C0644 $size ${file.getFileName}"
            writeLine(out, sizeCommand)
          }
          def sendFileAndTerminator() = withErrorCheck {
            fileToStream(file, out, subject)

            // sends '\0'
            val arr = new Array[Byte](1024)
            arr(0) = 0.toByte
            out.write(arr, 0, 1)
            out.flush()
          }
          def withErrorCheck(f: => Any): Either.RightProjection[String, Int] = {
            f
            findError(in).right
          }
          val result = for {
            _ <- connect()
            _ <- sendLastMod()
            _ <- sendSize()
            e <- sendFileAndTerminator()
          } yield e
          val errorOpt = result.left.toOption
          errorOpt.fold(subject.onCompleted())(error => subject onError new Exception(error))
        })
      })
    }.recoverAll(t => subject onError t)

    subject
  }

  /**
   * Executes `command` on the remote machine.
   *
   * @see http://www.jcraft.com/jsch/examples/Exec.java.html
   *
   * @param command command to execute
   * @return the response
   */
  def execute(command: String, more: String*): CommandResponse = {
    val channel = openExecChannel(session, command)
    val ret = handleCommandStream(channel, more)
    ret.exitValue.onComplete(_ => channel.disconnect())
    ret
  }

  /**
   *
   * @see `execute(String, String*)`
   * @return the result, once the command has run to completion
   */
  def executeAwait(command: String, more: String*): CommandResult = execute(command, more: _*).await()

  /**
   * First runs `sudo -s -S`, then supplies the password, then executes `command`, followed by any `more` and finally
   * `exit`.
   *
   * (Without running `exit` at the end, the response would never complete.)
   *
   * @param sudoPassword the sudo password
   * @param command command to run as root
   * @param more additional root commands
   * @return the response
   */
  def executeAsSudo(sudoPassword: String)(command: String, more: String*): CommandResponse = {
    val afterSudo = sudoPassword :: command :: more.toList ::: "exit" :: Nil
    execute("sudo -s -S", afterSudo: _*)
  }

  private def shell() {
    val channel = session openChannel SHELL
    channel.setInputStream(System.in)
    channel.setOutputStream(System.out)
    channel.connect()
    channel.disconnect()
  }

  private def writeLine(out: OutputStream, content: String) = {
    out.write(s"$content\n".getBytes)
    out.flush()
  }

  private def findError(in: InputStream): Either[String, Int] = {
    val b = in.read()
    if (b == 1 || b == 2) {
      val errorMessage = Iterator.continually(in.read().toChar).takeWhile(_ != '\n').mkString
      Left(errorMessage)
    } else {
      Right(b)
    }
  }

  private def fileToStream(file: Path, out: OutputStream, subject: Subject[StorageSize]) = {
    var sent = 0L
    Utils.using(new FileInputStream(file.toFile))(fis => {
      val buf = new Array[Byte](1024)
      var stop = false
      while (!stop) {
        val len = fis.read(buf, 0, buf.length)
        if (len < 0) {
          stop = true
        } else {
          out.write(buf, 0, len)
          sent += len
          subject onNext sent.bytes
        }
      }
    })
  }

  private def openExecChannel(session: Session, command: String) = {
    val channel = (session openChannel EXEC).asInstanceOf[ChannelExec]
    channel setCommand command
    channel
  }

  private def handleCommandStream(channel: ChannelExec, userInput: Seq[String]): CommandResponse = {
    val exitPromise = Promise[Int]()
    val standardOut = ReplaySubject[String]()
    val errorOut = new ReplayRxOutputStream()
    val out = channel.getOutputStream
    channel setErrStream errorOut
    channel.connect()
    userInput.foreach(writeLine(out, _))
    // reads the response in another thread
    completeAsync(channel, exitPromise, standardOut, errorOut).recoverAll(t => {
      exitPromise tryFailure t
      standardOut onError t
      errorOut onError t
      errorOut.close()
    })
    CommandResponse(exitPromise.future, standardOut, errorOut.lines)
  }

  /**
   * Reads the response asynchronously, eventually completing the provided Promise, Subject and OutputStream.
   *
   * @param channel channel
   * @param exit promise of exit value
   * @param standardOut standard out subject
   * @param errorOut error output stream
   */
  private def completeAsync(channel: ChannelExec,
                            exit: Promise[Int],
                            standardOut: Subject[String],
                            errorOut: RxOutputStream): Future[Unit] = {
    Future {
      val in = channel.getInputStream
      val bufferSize = 1024
      val buf = new Array[Byte](bufferSize)
      var stop = false
      //      val builder = new StringBuilder
      while (!stop) {
        while (in.available() > 0 && !stop) {
          val len = in.read(buf, 0, bufferSize)
          if (len < 0) {
            // hmm
          } else {
            val str = new String(buf, 0, len)
            // I believe the last char in `str` is \n
            val content = if (str endsWith "\n") str.init else str
            content.split("\n", -1).foreach(standardOut.onNext)
            //            RxHelper.emitFullLines(str, builder, standardOut)
          }
        }
        if (channel.isClosed) {
          if (in.available() == 0) {
            stop = true
            exit success channel.getExitStatus
            standardOut.onCompleted()
            errorOut.close()
          }
        } else {
          Thread.sleep(100)
        }
      }
    }
  }

  private def initSession(): Session = {
    val jsch = new JSch
    creds.onInit(jsch)
    val session = jsch.getSession(user, host, port)
    val keyCheckingValue = if (strictKeyChecking) "yes" else "no"
    session.setConfig("StrictHostKeyChecking", keyCheckingValue)
    session.setUserInfo(DefaultUserInfo)
    creds.onSession(session)
    session.connect()
    session
  }

  private def withChannel[T <: Channel, U](c: T)(f: T => U): U =
    try {
      f(c)
    } finally {
      c.disconnect()
    }

  override def close(): Unit = {
    session.disconnect()
    executor.shutdown()
  }
}