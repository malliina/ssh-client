package com.mle.streams

import rx.lang.scala.{Observable, Subject}

/**
 * @author Michael
 */
object RxHelper {
  /**
   * Emits any complete lines in `input` to `subject` and appends any remainder to `builder`.
   *
   * The first emitted line (i.e. the first line in `lines`) is prepended by any existing content in `builder`.
   *
   * @param input a possibly multi-line input
   * @param builder string builder
   * @param subject subject that emits complete lines
   */
  def emitFullLines(input: String, builder: StringBuilder, subject: Subject[String]): Unit = {
    val (lines, remainder) = splitLines(input)
    if (lines.nonEmpty) {
      builder append lines.head
      subject onNext builder.toString()
      builder.clear()
      lines.tail foreach (s => subject.onNext(s))
    }
    //    println(s"Remainder: $remainder")
    builder append remainder
  }

  def splitLines(input: String, sep: String = "\n"): (Seq[String], String) = {
    val lines: Array[String] = input.split(sep, -1)
    lines.size match {
      case 1 => (Seq.empty, lines.head)
      case i if i > 1 => (lines.take(i - 1), lines.last)
    }
  }

  /**
   * Returns an [[Observable]] that parses `obs` and emits complete lines whenever a '\n' is encountered in `obs`. When
   * `obs` completes or errors, the returned [[Observable]] will emit any remaining items as one last string before
   * completing itself.
   *
   * @param obs
   * @return an [[Observable]] that parses `obs` and emits complete lines whenever a '\n' is encountered in `obs`
   */
  def bufferLines(obs: Observable[Char]) = Observable[String](subscriber => {
    val buffer = new StringBuilder

    def emitAndClearBuffer(emitIfEmpty: Boolean) = {
      val line = buffer.toString()
      if (line.nonEmpty || emitIfEmpty) {
        subscriber onNext line
      }
      buffer.clear()
    }
    val subscription = obs.subscribe(
      c => if (c == '\n') {
        emitAndClearBuffer(emitIfEmpty = true)
      } else {
        buffer.append(c)
      },
      err => {
        emitAndClearBuffer(emitIfEmpty = false)
        subscriber.onError(err)
      },
      () => {
        emitAndClearBuffer(emitIfEmpty = false)
        subscriber.onCompleted()
      }
    )
    subscriber add subscription
  })
}
