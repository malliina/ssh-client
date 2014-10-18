package com.mle.streams

import java.io.OutputStream
import java.math.BigInteger

import rx.lang.scala.Subject

/**
 * @author Michael
 */
class RxOutputStream(val subject: Subject[Char]) extends OutputStream {
  val lines = RxHelper.bufferLines(subject)

  override def write(b: Int): Unit = {
    val arr = BigInteger.valueOf(b).toByteArray
    write(arr, 0, arr.length)
  }

  override def write(bytes: Array[Byte], offset: Int, byteCount: Int): Unit = {
    val str = new String(bytes, offset, byteCount)
    str.foreach(subject.onNext)
  }
}