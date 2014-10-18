package com.mle.streams

import rx.lang.scala.subjects.ReplaySubject

/**
 * @author Michael
 */
class ReplayRxOutputStream extends RxOutputStream(ReplaySubject[Char]()) {
  def onNext(value: Char) = subject onNext value

  def onError(t: Throwable) = subject onError t

  override def close(): Unit = {
    super.close()
    subject.onCompleted()
  }
}