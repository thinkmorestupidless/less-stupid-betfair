package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.navigation.domain._

object MenuUtils {

  implicit class MenuUtilsMenuOps(self: Menu) {

    def allEvents: List[Event] = _allEvents(self)

    def findEventByName(eventName: EventName): Option[Event] =
      self.allEvents.find(_.name == eventName)
  }

  implicit class MenuUtilsEventOps(self: Event) {

    def allEvents: List[Event] = _allEvents(self)

    lazy val allMarkets: List[Market] = {
      def fold(markets: List[Market], next: MenuItem): List[Market] =
        next match {
          case m: Market      => markets :+ m
          case h: HasChildren => h.children.foldLeft(markets)(fold)
        }

      self.children.foldLeft(List.empty[Market])(fold)
    }

    def getMarketsWithMarketType(marketType: MarketType): List[Market] =
      self.allMarkets.filter(_.marketType == marketType)
  }

  implicit class MenuUtilsEventTypeOps(self: EventType) {

    def allEvents: List[Event] = _allEvents(self)
  }

  private def _allEvents(parent: HasChildren): List[Event] = {
    def fold(events: List[Event], next: MenuItem): List[Event] = {
      next match {
        case event: Event        => event.children.foldLeft(events :+ event)(fold)
        case parent: HasChildren => parent.children.foldLeft(events)(fold)
        case _                   => events
      }
    }

    parent.children.foldLeft(List.empty[Event])(fold)
  }

}
