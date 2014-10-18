package com.mle.ssh

import rx.lang.scala.Observable

import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.{Await, Future}

/**
 * @author Michael
 */
case class CommandResponse(exitValue: Future[Int], standardOut: Observable[String], errorOut: Observable[String]) {
  def output = standardOut merge errorOut

  def await(timeout: Duration = 10.seconds): CommandResult = {
    val exit = Await.result(exitValue, timeout)
    val standard = standardOut.toBlocking.toList
    val error = errorOut.toBlocking.toList
    val out = output.toBlocking.toList
    CommandResult(exit, standard, error, out)
  }

  //  def isSuccess = exitValue.map(_ == 0)
  //
  //  standardOut.toIterable
  //
  //  def map(f: Int => CommandResponse) = exitValue.map(f)
}