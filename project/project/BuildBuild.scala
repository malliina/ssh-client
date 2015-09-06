import sbt._

object BuildBuild extends Build {

  override lazy val settings = super.settings ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.github.malliina" %% "sbt-utils" % "0.2.1"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}