import com.mle.sbtutils.{SbtProjects, SbtUtils}
import sbt.Keys._
import sbt._

/**
 *
 */
object SshBuild extends Build {

  lazy val sshProject = SbtProjects.mavenPublishProject("ssh-client").settings(projectSettings: _*)

  lazy val projectSettings = Seq(
    version := "0.0.4",
    sbtPlugin := true,
    organization := "com.github.malliina",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    scalaVersion := "2.11.2",
    crossScalaVersions := Seq("2.10.4", "2.11.2"),
    fork in Test := true,
    libraryDependencies ++= Seq(
      "com.jcraft" % "jsch" % "0.1.51",
      "io.reactivex" % "rxscala_2.11" % "0.21.1",
      "com.github.malliina" %% "util" % "1.4.2") map (_ withSources()),
    // includes scala-xml for 2.11 but excludes it for 2.10
    // see http://www.scala-lang.org/news/2014/03/06/release-notes-2.11.0-RC1.html
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
        case _ =>
          libraryDependencies.value
      }
    }
  )
}