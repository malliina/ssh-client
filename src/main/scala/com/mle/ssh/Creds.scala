package com.mle.ssh

import java.nio.file.Path

import com.jcraft.jsch.{JSch, Session}

/**
 * @author Michael
 */
trait Creds {
  def onInit(ssh: JSch): Unit

  def onSession(session: Session): Unit
}

object Creds {

  case class KeyCreds(key: Path, passPhrase: Option[String] = None) extends Creds {
    private val keyPath = key.toFile.getAbsolutePath

    override def onInit(ssh: JSch): Unit =
      passPhrase.fold(ssh addIdentity keyPath)(phrase => ssh addIdentity(keyPath, phrase))

    override def onSession(session: Session): Unit = ()
  }

  case class PassCreds(pass: String) extends Creds {

    override def onInit(ssh: JSch): Unit = ()

    override def onSession(session: Session): Unit = session setPassword pass
  }

}