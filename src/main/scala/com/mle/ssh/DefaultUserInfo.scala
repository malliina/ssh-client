package com.mle.ssh

import com.mle.util.Log
import com.jcraft.jsch.UserInfo

/**
 * @author Michael
 */
object DefaultUserInfo extends DefaultUserInfo

class DefaultUserInfo extends UserInfo with Log {
  override def getPassphrase: String = {
    log info "Wanted passphrase, returning null"
    null
  }

  override def promptPassword(message: String): Boolean = {
    log info s"Returning false to password prompt: $message"
    false
  }

  override def promptYesNo(message: String): Boolean = {
    log info s"Returning false to: $message"
    false
  }

  override def showMessage(message: String): Unit = {
    log info s"Message: $message"
  }

  override def getPassword: String = {
    log info "Wanted password, returning null"
    null
  }

  override def promptPassphrase(message: String): Boolean = {
    log info s"Returning false to passphrase prompt: $message"
    false
  }
}
