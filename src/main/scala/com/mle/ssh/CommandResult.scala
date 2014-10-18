package com.mle.ssh

/**
 * @author Michael
 */
case class CommandResult(exitValue: Int, standardOut: Seq[String], errorOut: Seq[String], out: Seq[String]) {
  def prettyPrint: String = {
    val outputLines = out.mkString("Output:\n\t", "\n\t", "\n")
    val exitLine = s"Exit value: $exitValue"
    s"$outputLines$exitLine"
  }

  def prettyPrintSeparated: String = {
    val standard = standardOut.mkString("Standard out:\n\t", "\n\t", "\n")
    val error = errorOut.mkString("Error out:\n\t", "\n\t", "\n")
    val exitLine = s"Exit value: $exitValue"
    s"$standard$error$exitLine"
  }

  override def toString: String = prettyPrint
}