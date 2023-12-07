package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.streams.domain.{
  BetfairSocketMessage,
  IncomingBetfairSocketMessage,
  OutgoingBetfairSocketMessage
}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{Broadcast, Flow, GraphDSL}
import org.apache.pekko.stream.{FanOutShape, Graph, Inlet, Outlet}

object BetfairMessageSplitter {

  def apply(): Graph[BetfairMessageFanOutShape, NotUsed] =
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[BetfairSocketMessage](outputPorts = 2))
      val incomingOut = b.add(Flow[BetfairSocketMessage].collect { case incoming: IncomingBetfairSocketMessage =>
        incoming
      })
      val outgoingOut = b.add(Flow[BetfairSocketMessage].collect { case outgoing: OutgoingBetfairSocketMessage =>
        outgoing
      })

      broadcast ~> incomingOut
      broadcast ~> outgoingOut

      new BetfairMessageFanOutShape(broadcast.in, incomingOut.out, outgoingOut.out)
    }
}

class BetfairMessageFanOutShape(_init: FanOutShape.Init[BetfairSocketMessage])
    extends FanOutShape[BetfairSocketMessage](_init) {
  def this(name: String) = this(FanOutShape.Name[BetfairSocketMessage](name))

  def this(
      in: Inlet[BetfairSocketMessage],
      incoming: Outlet[IncomingBetfairSocketMessage],
      outgoing: Outlet[OutgoingBetfairSocketMessage]
  ) = this(FanOutShape.Ports(in, incoming :: outgoing :: Nil))

  override protected def construct(init: FanOutShape.Init[BetfairSocketMessage]): FanOutShape[BetfairSocketMessage] =
    new BetfairMessageFanOutShape(init)

  override def deepCopy(): BetfairMessageFanOutShape = super.deepCopy().asInstanceOf[BetfairMessageFanOutShape]

  val incoming: Outlet[IncomingBetfairSocketMessage] = newOutlet[IncomingBetfairSocketMessage]("incoming")
  val outgoing: Outlet[OutgoingBetfairSocketMessage] = newOutlet[OutgoingBetfairSocketMessage]("outgoing")
}
