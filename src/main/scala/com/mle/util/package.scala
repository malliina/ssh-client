package com.mle

import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
package object util {

  implicit class RichTry[T](value: Try[T]) {
    def recoverAll[U >: T](fix: Throwable => U): Try[U] = value.recover {
      case t: Throwable => fix(t)
    }

    def fold[U](error: Throwable => U)(success: T => U) = value match {
      case Success(v) => success(v)
      case Failure(t) => error(t)
    }
  }

}
