package tests

import java.nio.file.Path

import com.mle.storage.StorageSize

import scala.concurrent.duration.DurationLong

/**
 * @author Michael
 */
class Tests extends BaseTests {
  test("SSH") {
    withClient(ssh => {
      printResponse(ssh.execute("sudo -s -S", confOpt.map(_.rootPassword).getOrElse(""), "ls /root", "exit"))
    })
  }
  test("SCP") {
    withClient(ssh => {
      printResponse(ssh execute "ls")
      println("SCP...")
      //      ssh.scp(Paths.get("E:\\") / "test.txt", "test.txt")
      println("SCP done")
      printResponse(ssh execute "ls")
    })
  }
  test("SCP ver 2") {
    val filePathOpt: Option[Path] = None // Some("E:\\pi.jar")
    filePathOpt.foreach(testFile => {
      val fileName = testFile.getFileName.toString
      withClient(ssh => {
        val transfer = ssh.scp(testFile, fileName)
        val sub = transfer.sample(1.seconds).subscribe(new PrintingObserver[StorageSize])
        transfer.toBlocking.last
        sub.unsubscribe()
        import com.mle.util.RichTry
        val msg = ssh.scpAwait(testFile, fileName).fold(t => s"Error: $t")(size => s"Transferred: $size")
        println(msg)
      })
    })

  }
  test("String.split") {
    val str = "this\nis\na\ntest\n"
    val lines = str.split("\n", -1)
    assert(lines.last.isEmpty)

    val str2 = "no newlines here"
    val lines2 = str2.split("\n", -1)
    assert(lines2.size == 1)
    assert(lines2.head === str2)

    val str3 = "one\nnewline"
    val lines3 = str3.split("\n", -1)
    assert(lines3.last === "newline")

    val str4 = ""
    val lines4 = str4.split("\n", -1)
    assert(lines4 === Array(""))

    assert(Seq("hey").take(0) == Seq.empty)
  }
}
