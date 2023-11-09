package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.navigation.domain._
import org.apache.pekko.http.SprayJsonSupport
import pl.iterators.kebs.json.KebsEnumFormats.jsonEnumFormat
import spray.json._

trait JsonCodecs extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val menuItemTypeFormat: JsonFormat[MenuItemType] = jsonEnumFormat

  implicit class JsValueVectorOps(self: Vector[JsValue]) {

    def asEventTypes(implicit reader: JsonReader[EventType]): List[EventType] =
      asListOfType[EventType](typeName = "EVENT_TYPE")

    def asEvents(implicit reader: JsonReader[Event]): List[Event] =
      asListOfType[Event](typeName = "EVENT")

    def asGroups(implicit reader: JsonReader[Group]): List[Group] =
      asListOfType[Group](typeName = "GROUP")

    def asRaces(implicit reader: JsonReader[Race]): List[Race] =
      asListOfType[Race](typeName = "RACE")

    def asMarkets(implicit reader: JsonReader[Market]): List[Market] =
      asListOfType[Market](typeName = "MARKET")

    private def asListOfType[T <: MenuItem](typeName: String)(implicit reader: JsonReader[T]): List[T] =
      self.map { child =>
        child.asJsObject.fields("type") match {
          case JsString(`typeName`) => Some(child.convertTo[T])
          case _ => None
        }
      }.flatten.toList
  }

  implicit val eventTypeFormat: RootJsonFormat[EventType] = new RootJsonFormat[EventType] {
    override def read(json: JsValue): EventType =
      json.asJsObject.getFields(fieldNames = "id", "name", "children") match {
        case Seq(JsString(id), JsString(name), JsArray(children)) =>
          EventType(EventTypeId(id), EventTypeName(name), children.asEvents, children.asGroups, children.asRaces)
      }

    override def write(obj: EventType): JsValue =
      JsObject(
        ("type", JsString(MenuItemType.EventType.toString)),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("children", JsArray((obj.events.map(_.toJson)  ++ obj.groups.map(_.toJson) ++ obj.races.map(_.toJson)).toVector))
      )
  }

  implicit val groupWriter: RootJsonWriter[Group] = (obj: Group) => JsObject(
    ("type", JsString(MenuItemType.Group.entryName)),
    ("id", JsString(obj.id.value)),
    ("name", JsString(obj.name.value)),
    ("children", JsArray((obj.events.map(_.toJson) ++ obj.groups.map(_.toJson)).toVector))
  )

  implicit val groupReader: RootJsonReader[Group] = (json: JsValue) => json.asJsObject.getFields(fieldNames = "id", "name", "children") match {
    case Seq(JsString(id), JsString(name), JsArray(children)) =>
      Group(GroupId(id), GroupName(name), children.asEvents, children.asGroups)
  }

  implicit val groupFormat: RootJsonFormat[Group] = rootJsonFormat(groupReader, groupWriter)

  implicit val eventWriter: RootJsonWriter[Event] = (obj: Event) => JsObject(
    ("type", JsString(MenuItemType.Event.entryName)),
    ("id", JsString(obj.id.value)),
    ("name", JsString(obj.name.value)),
    ("countryCode", JsString(obj.countryCode.value)),
    ("children", JsArray((obj.events.map(_.toJson) ++ obj.groups.map(_.toJson) ++ obj.markets.map(_.toJson)).toVector))
  )

  implicit val eventReader: RootJsonReader[Event] = (json: JsValue) =>
    json.asJsObject.getFields(fieldNames = "id", "name", "countryCode", "children") match {
      case Seq(JsString(id), JsString(name), JsString(countryCode), JsArray(children)) =>
        Event(EventId(id), EventName(name), CountryCode(countryCode), children.asEvents, children.asGroups, children.asMarkets)
    }

  implicit val eventFormat: RootJsonFormat[Event] = rootJsonFormat(eventReader, eventWriter)

  implicit val marketFormat: RootJsonFormat[Market] = new RootJsonFormat[Market] {
    override def read(json: JsValue): Market =
      json.asJsObject.getFields(fieldNames = "id", "name", "exchangeId", "marketType", "marketStartTime", "numberOfWinners") match {
        case Seq(
              JsString(id),
              JsString(name),
              JsString(exchangeId),
              JsString(marketType),
              JsString(marketStartTime),
              numberOfWinners: JsValue
            ) =>
          Market(
            MarketId(id),
            MarketName(name),
            ExchangeId(exchangeId),
            MarketType(marketType),
            MarketStartTime(marketStartTime),
            readNumberOfWinners(numberOfWinners)
          )
        case _ => deserializationError(msg = s"Couldn't parse Market from '$json'")
      }

    private def readNumberOfWinners(json: JsValue): NumberOfWinners =
      json match {
        case JsNumber(n)  => NumberOfWinners(value = Some(n.intValue))
        case JsString("") => NumberOfWinners(value = None)
        case _            => deserializationError(msg = s"Unable to read NumberOfWinners from `$json`")
      }

    override def write(obj: Market): JsValue =
      JsObject(
        ("type", JsString("MARKET")),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("exchangeId", JsString(obj.exchangeId.value)),
        ("marketType", JsString(obj.marketType.value)),
        ("marketStartTime", JsString(obj.marketStartTime.value)),
        ("numberOfWinners", writeNumberOfWinners(obj.numberOfWinners))
      )

    private def writeNumberOfWinners(numberOfWinners: NumberOfWinners): JsValue =
      numberOfWinners.value match {
        case Some(n) => JsNumber(n)
        case None    => JsString("")
      }
  }

  implicit val raceFormat: RootJsonFormat[Race] = new RootJsonFormat[Race] {
    override def read(json: JsValue): Race =
      json.asJsObject.getFields(fieldNames = "id", "name", "countryCode", "venue", "startTime", "children") match {
        case Seq(
              JsString(id),
              JsString(name),
              JsString(countryCode),
              JsString(venue),
              JsString(startTime),
              JsArray(children)
            ) =>
          Race(
            RaceId(id),
            RaceName(name),
            CountryCode(countryCode),
            Venue(venue),
            RaceStartTime(startTime),
            children.asMarkets
          )
        case _ => deserializationError(msg = s"Unable to read Race from '$json'")
      }

    override def write(obj: Race): JsValue =
      JsObject(
        ("type", JsString("RACE")),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("countryCode", JsString(obj.countryCode.value)),
        ("venue", JsString(obj.venue.value)),
        ("startTime", JsString(obj.startTime.value)),
        ("children", JsArray(obj.markets.map(_.toJson).toVector))
      )
  }

  implicit val menuFormat: RootJsonFormat[Menu] = new RootJsonFormat[Menu] {
    override def read(json: JsValue): Menu =
      json.asJsObject.getFields(fieldNames = "type", "id", "name", "children") match {
        case Seq(JsString("GROUP"), JsNumber(id), JsString("ROOT"), JsArray(children)) if id == 0 =>
          Menu(children.asEventTypes)
        case x =>
          deserializationError(
            msg = s"""Expected '{"type": "GROUP","name": "ROOT","id": 0,"children": []}' but received '$x'"""
          )
      }

    override def write(obj: Menu): JsValue =
      JsObject(
        ("type", JsString("GROUP")),
        ("name", JsString("ROOT")),
        ("id", JsNumber(0)),
        ("children", writeChildren(obj.children))
      )

    private def writeChildren(children: List[MenuItem]): JsValue =
      JsArray(children.map {
        case e: EventType => e.toJson
        case x            => deserializationError(msg = s"Menu can only have EventTypes as children - but found '$x'")
      }.toVector)
  }
}

object JsonCodecs extends JsonCodecs
