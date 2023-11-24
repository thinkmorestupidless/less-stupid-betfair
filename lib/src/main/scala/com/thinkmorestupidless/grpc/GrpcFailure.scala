package com.thinkmorestupidless.grpc

final case class GrpcFailure(message: String) extends RuntimeException(message)
