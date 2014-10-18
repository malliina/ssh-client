package com.mle.ssh

import java.nio.file.{Path, Paths}

import com.mle.file.{FileUtilities, StorageFile}
import com.mle.util.BaseConfigReader

/**
 * @author Michael
 */
object RemoteConfigReader extends BaseConfigReader[RootRemoteInfo] {
  override def userHomeConfPath: Path = FileUtilities.userHome / "keys" / "remote.conf"

  override def loadOpt: Option[RootRemoteInfo] = fromUserHomeOpt

  override def fromMapOpt(map: Map[String, String]): Option[RootRemoteInfo] =
    for {
      host <- map get "host"
      port <- (map get "port").map(_.toInt) orElse Some(22)
      user <- map get "user"
      key <- map get "key"
      rootPassword <- map get "rootPassword"
    } yield RootRemoteInfo(host, port, user, Paths get key, rootPassword)
}

case class RootRemoteInfo(host: String, port: Int, user: String, key: Path, rootPassword: String)
