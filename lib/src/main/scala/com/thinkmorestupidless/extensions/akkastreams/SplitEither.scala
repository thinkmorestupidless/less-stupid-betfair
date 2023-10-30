package com.thinkmorestupidless.extensions.akkastreams

import akka.NotUsed
import akka.stream.Graph
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL}

object SplitEither {

  def apply[L, R]: Graph[EitherFanOutShape[Either[L, R], L, R], NotUsed] =
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[Either[L, R]](outputPorts = 2))
      val leftOut = b.add(Flow[Either[L, R]].collect { case Left(l) => l })
      val rightOut = b.add(Flow[Either[L, R]].collect { case Right(r) => r })

      broadcast ~> leftOut
      broadcast ~> rightOut

      new EitherFanOutShape(broadcast.in, leftOut.out, rightOut.out)
    }
}

import akka.stream.{FanOutShape, Inlet, Outlet}

class EitherFanOutShape[In, L, R](_init: FanOutShape.Init[In]) extends FanOutShape[In](_init) {
  def this(name: String) = this(FanOutShape.Name[In](name))
  def this(in: Inlet[In], left: Outlet[L], right: Outlet[R]) = this(FanOutShape.Ports(in, left :: right :: Nil))

  override protected def construct(init: FanOutShape.Init[In]): FanOutShape[In] = new EitherFanOutShape(init)
  override def deepCopy(): EitherFanOutShape[In, L, R] = super.deepCopy().asInstanceOf[EitherFanOutShape[In, L, R]]

  val left: Outlet[L] = newOutlet[L]("left")
  val right: Outlet[R] = newOutlet[R]("right")
}
