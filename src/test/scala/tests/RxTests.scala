package tests

import com.mle.streams.RxHelper
import org.scalatest.FunSuite
import rx.lang.scala.Observable

import scala.concurrent.Future

/**
 * @author Michael
 */
class RxTests extends FunSuite {
  val in = Seq('a', 'b', '\n', 'c', 'd', '\n', 'e')
  val charObs = Observable.from(in)

  //  test("Observable[Char] to Observable[String]") {
  //    val oneSource = charObs.publish.refCount
  //    val separators = oneSource.filter(_ == '\n')
  //    val lines = oneSource.tumblingBuffer(separators).map(chars => new String(chars.toArray))
  //    assert(lines.toBlocking.toList === List("ab", "cd", "e"))
  //  }
  //  test("Observable[Char] to Observable[String], using scan") {
  //    case class Part(acc: String)
  //    val eitherObs = charObs.scan[Either[Part, String]](Left(Part("")))((e, c) => if (c == '\n') Right(e.fold(_.acc, _ => "")) else Left(Part(e.fold(p => p.acc + c, _ => "" + c))))
  //    val lines = eitherObs.filter(_.isRight).map(_.right.get)
  //    assert(lines.toBlocking.toList === List("ab", "cd", "e"))
  //  }
  test("Observable[Char] to Observable[String] using Observable.apply") {
    val lines = RxHelper.bufferLines(charObs)
    assert(lines.toBlocking.toList === List("ab", "cd", "e"))
  }
  test("concat, map"){
    val f = Future(5)
    f.map(_ => ())
  }
}
