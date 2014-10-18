# ssh-client #

This SSH client library provides scala.concurrent.Futures and rx.lang.scala.Observables as responses to remotely 
executed commands. SCP file transfers are also supported. The underlying SSH library is Jsch.

## Usage ##

### Execute a Command ###

```
import com.mle.ssh.{CommandResponse, CommandResult, SSH}
import java.nio.file.Path
val keyFile: Path = ???
val ssh = new SSH("10.0.0.1", 22, "michael", keyFile)
val response: CommandResponse = ssh execute "ls"
// case class CommandResponse(exitValue: Future[Int], standardOut: Observable[String], errorOut: Observable[String])
val blockingResponse: CommandResult = response.await()
// case class CommandResult(exitValue: Int, standardOut: Seq[String], errorOut: Seq[String], out: Seq[String])
ssh.close()
```

### Execute a Command, using sudo ###

```
import com.mle.ssh.{CommandResponse, CommandResult, SSH}
import java.nio.file.Path
val keyFile: Path = ???
val sudoPassword: String = ???
val ssh = new SSH("10.0.0.1", 22, "michael", keyFile)
val response: CommandResponse = ssh.executeAsSudo(sudoPassword)("useradd -m -s /bin/bash newusername")
response.await()
ssh.close()
```

### Transfer a File ###

```
import com.mle.storage.StorageSize
import java.nio.file.Path
import rx.lang.scala.Observable
val testFile: Path = ???
val destination = testFile.getFileName.toString
val ssh = new SSH("10.0.0.1", 22, "michael", keyFile)
val transfer: Observable[StorageSize] = ssh.scp(testFile, destination)
```