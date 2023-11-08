package com.thinkmorestupidless.betfair.navigation.impl.grpc

import com.thinkmorestupidless.betfair.navigation.domain.usecases.GetMenuUseCase.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.impl.grpc.Decoders._
import com.thinkmorestupidless.betfair.navigation.impl.grpc.Encoders._
import com.thinkmorestupidless.betfair.proto.navigation.{GetMenuRequest, Menu => MenuProto, NavigationService}
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.grpc.Encoder._

import scala.concurrent.{ExecutionContext, Future}

final case class GrpcError(message: String) extends RuntimeException(message)

class GprcNavigationService(getMenu: GetMenuUseCase)(implicit ec: ExecutionContext) extends NavigationService {

  override def getMenu(in: GetMenuRequest): Future[MenuProto] =
    in.decode.fold(
      error => Future.failed(GrpcError("Kablammo")),
      request =>
        getMenu(request).flatMap {
          case Right(menu) =>
            Future.successful(menu.encode)
          case Left(error) =>
            Future.failed(GrpcError(error.message))
        }
    )
}
