# ssh-client #

This SSH client library provides scala.concurrent.Futures and rx.lang.scala.Observables as responses to remotely 
executed commands. SCP file transfers are also supported. The underlying SSH library is Jsch.

## Usage ##

### Execute a Command ###

```
val keyFile: Path = ???
val ssh = new SSH("10.0.0.1", 22, "michael", keyFile)
val response: CommandResponse = ssh execute "ls"
// case class CommandResponse(exitValue: Future[Int], standardOut: Observable[String], errorOut: Observable[String])
val blockingResponse: CommandResult = response.await()
// case class CommandResult(exitValue: Int, standardOut: Seq[String], errorOut: Seq[String], out: Seq[String])
ssh.close()
```

### Transfer a File ###

```
val testFile: Path = ???
val destination = testFile.getFileName.toString
val transfer: Observable[StorageSize] = ssh.scp(testFile, destination)
```