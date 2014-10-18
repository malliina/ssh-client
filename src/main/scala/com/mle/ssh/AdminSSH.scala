package com.mle.ssh

import java.nio.file.Path

import com.mle.file.FileUtilities

/**
 * @author Michael
 */
class AdminSSH(conf: RootRemoteInfo) extends SSH(conf.host, conf.port, conf.user, conf.key) {
  def addUser(user: String, authorizedKeyFile: Path): CommandResponse =
    addUser(user, FileUtilities.fileToString(authorizedKeyFile))

  def addUser(user: String, authorizedkey: String): CommandResponse = {
    val sshDir = s"/home/$user/.ssh"
    val authKeysFile = s"$sshDir/authorized_keys"
    executeAsSudo(conf.rootPassword)(
      s"useradd -m -s /bin/bash $user",
      s"mkdir $sshDir",
      s"chmod 700 $sshDir",
      s"chown $user:$user $sshDir",
      s"touch $authKeysFile",
      s"chmod 600 $authKeysFile",
      s"chown $user:$user $authKeysFile",
      s"""echo "$authorizedkey" >> $authKeysFile""")
  }

  def deleteUser(user: String): CommandResponse = executeAsSudo(conf.rootPassword)(s"deluser $user")
}