package tests

import com.mle.ssh.{CommandResponse, CommandResult, RemoteConfigReader, SSH}
import com.mle.util.Utils
import org.scalatest.FunSuite
import rx.lang.scala.Observer

/**
 * @author Michael
 */
class BaseTests extends FunSuite {
  val confOpt = RemoteConfigReader.loadOpt

  def withClient[T](f: SSH => T): Option[T] = {
    confOpt.map(conf => Utils.using[SSH, T](new SSH(conf.host, conf.port, conf.user, conf.key))(f))
  }

  def printResponse(cmd: CommandResponse, printer: CommandResult => String = _.prettyPrint) =
    printResult(cmd.await(), printer)

  def printResult(result: CommandResult, printer: CommandResult => String = _.prettyPrint) =
    println(printer(result))

  class PrintingObserver[T] extends Observer[T] {
    override def onNext(value: T): Unit = println(s"Value: $value")

    override def onError(error: Throwable): Unit = println(s"Error: ${error.getMessage}. $error")

    override def onCompleted(): Unit = println(s"Completed.")
  }

}
