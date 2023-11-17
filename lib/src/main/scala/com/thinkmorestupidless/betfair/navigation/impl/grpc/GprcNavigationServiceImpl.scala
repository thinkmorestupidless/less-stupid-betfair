package com.thinkmorestupidless.betfair.navigation.impl.grpc

import com.thinkmorestupidless.betfair.navigation.domain.usecases.GetMenuUseCase.GetMenuUseCase
import com.thinkmorestupidless.betfair.navigation.impl.grpc.Decoders._
import com.thinkmorestupidless.betfair.navigation.impl.grpc.Encoders._
import com.thinkmorestupidless.betfair.proto.navigation.{GetMenuRequest, NavigationService, Menu => MenuProto}
import com.thinkmorestupidless.grpc.Decoder._
import com.thinkmorestupidless.grpc.Encoder._
import com.thinkmorestupidless.utils.ValidationException

import scala.concurrent.{ExecutionContext, Future}

class GprcNavigationServiceImpl(getMenu: GetMenuUseCase)(implicit ec: ExecutionContext) extends NavigationService {

  override def getMenu(in: GetMenuRequest): Future[MenuProto] =
    in.decode.fold(
      errors => Future.failed(ValidationException.combineErrors(errors)),
      request =>
        getMenu(request).value.flatMap {
          case Right(menu) => Future.successful(menu.encode)
          case Left(error) => Future.failed(error)
        }
    )
}
