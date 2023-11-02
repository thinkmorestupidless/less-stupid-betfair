package com.thinkmorestupidless.betfair.examples

import com.thinkmorestupidless.betfair.examples.grpc.{GetEventsRequest, GetEventsResponse, GrpcExampleService}

import scala.concurrent.Future

final class GrpcExampleServiceImpl extends GrpcExampleService {
  override def getEvents(in: GetEventsRequest): Future[GetEventsResponse] = ???
}
