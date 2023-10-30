package com.thinkmorestupidless.utils

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import cats.data.{EitherT, OptionT}
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{ExecutionContext, Future}

trait FutureSupport extends ScalaFutures {

  protected def awaitTimeout: Timeout = Timeout(Span(10, Seconds))
  protected def awaitInterval: Interval = Interval(Span(10, Millis))

  def await[T](f: Future[T]): T =
    whenReady(f, awaitTimeout, awaitInterval)(identity)

  def await[T](opt: OptionT[Future, T]): Option[T] =
    await(opt.value)

  def awaitSeq[T](f: Future[T]*)(implicit ec: ExecutionContext): Seq[T] =
    await(Future.sequence(f))

  def awaitRight[E, T](e: EitherT[Future, E, T]): T =
    whenReady(e.value, awaitTimeout, awaitInterval) {
      case Left(error)   => throw new IllegalStateException(s"Expected Either.right, got Either.left [$error]")
      case Right(result) => result
    }

  def awaitLeft[E, T](e: EitherT[Future, E, T]): E =
    whenReady(e.value, awaitTimeout, awaitInterval) {
      case Left(error)   => error
      case Right(result) => throw new IllegalStateException(s"Expected Either.left, got Either.right [$result]")
    }

  def awaitSource[Out, Mat](source: Source[Out, Mat])(implicit mat: Materializer): Seq[Out] =
    await(source.runWith(Sink.seq))
}

object FutureSupport extends FutureSupport
