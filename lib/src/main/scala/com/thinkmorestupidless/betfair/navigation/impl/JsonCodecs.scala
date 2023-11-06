package com.thinkmorestupidless.betfair.navigation.impl

import com.thinkmorestupidless.betfair.navigation.domain._
import org.apache.pekko.http.SprayJsonSupport
import pl.iterators.kebs.json.KebsEnumFormats.jsonEnumFormat
import spray.json._

trait JsonCodecs extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val menuItemTypeFormat: JsonFormat[MenuItemType] = jsonEnumFormat

  implicit val eventTypeFormat: RootJsonFormat[EventType] = new RootJsonFormat[EventType] {
    override def read(json: JsValue): EventType =
      json.asJsObject.getFields("id", "name", "children") match {
        case Seq(JsString(id), JsString(name), JsArray(children)) =>
          EventType(EventTypeId(id), EventTypeName(name), readChildren(children))
      }

    private def readChildren(children: Vector[JsValue]): List[MenuItem] = {
      children.map { child =>
        child.asJsObject.fields("type") match {
          case JsString("EVENT") => child.convertTo[Event]
          case JsString("GROUP") => child.convertTo[Group]
          case JsString("RACE")  => child.convertTo[Race]
          case other             => deserializationError(s"Expected 'EVENT' or 'GROUP' or 'RACE' but received '$other'")
        }
      }
    }.toList

    override def write(obj: EventType): JsValue =
      JsObject(
        ("type", JsString(MenuItemType.EventType.toString)),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("children", writeChildren(obj.children)))

    private def writeChildren(children: List[MenuItem]): JsValue =
      JsArray(children.map {
        case child: Event => child.toJson
        case child: Group => child.toJson
        case child: Race  => child.toJson
      }.toVector)
  }

  implicit val groupWriter: RootJsonWriter[Group] = new RootJsonWriter[Group] {
    override def write(obj: Group): JsValue =
      JsObject(
        ("type", JsString(MenuItemType.Group.entryName)),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("children", writeChildren(obj.children)))

    private def writeChildren(children: List[MenuItem]): JsValue =
      JsArray(children.map {
        case child: Event => child.toJson
        case child: Group => child.toJson
      }.toVector)
  }

  implicit val groupReader: RootJsonReader[Group] = new RootJsonReader[Group] {
    override def read(json: JsValue): Group =
      json.asJsObject.getFields("id", "name", "children") match {
        case Seq(JsString(id), JsString(name), JsArray(children)) =>
          Group(GroupId(id), GroupName(name), readChildren(children))
      }

    private def readChildren(children: Vector[JsValue]): List[MenuItem] = {
      children.map { child =>
        child.asJsObject.fields("type") match {
          case JsString("EVENT") => child.convertTo[Event]
          case JsString("GROUP") => child.convertTo[Group]
          case other             => deserializationError(s"Expected 'EVENT' or 'GROUP' but received '$other'")
        }
      }
    }.toList
  }

  implicit val groupFormat: RootJsonFormat[Group] = rootJsonFormat(groupReader, groupWriter)

  implicit val eventWriter: RootJsonWriter[Event] = new RootJsonWriter[Event] {
    override def write(obj: Event): JsValue =
      JsObject(
        ("type", JsString(MenuItemType.Event.entryName)),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("countryCode", JsString(obj.countryCode.value)),
        ("children", writeChildren(obj.children)))

    private def writeChildren(children: List[MenuItem]): JsValue =
      JsArray(children.map {
        case child: Market => child.toJson
        case child: Event  => child.toJson
        case child: Group  => child.toJson
      }.toVector)
  }

  implicit val eventReader: RootJsonReader[Event] = new RootJsonReader[Event] {
    override def read(json: JsValue): Event =
      json.asJsObject.getFields("id", "name", "countryCode", "children") match {
        case Seq(JsString(id), JsString(name), JsString(countryCode), JsArray(children)) =>
          Event(EventId(id), EventName(name), CountryCode(countryCode), readChildren(children))
      }

    private def readChildren(children: Vector[JsValue]): List[MenuItem] = {
      children.map { child =>
        child.asJsObject.fields("type") match {
          case JsString("MARKET") => child.convertTo[Market]
          case JsString("EVENT")  => child.convertTo[Event]
          case JsString("GROUP")  => child.convertTo[Group]
          case other              => deserializationError(s"Expected 'EVENT' or 'GROUP' or 'MARKET' but received '$other'")
        }
      }.toList
    }
  }

  implicit val eventFormat: RootJsonFormat[Event] = rootJsonFormat(eventReader, eventWriter)

  implicit val marketFormat: RootJsonFormat[Market] = new RootJsonFormat[Market] {
    override def read(json: JsValue): Market =
      json.asJsObject.getFields("id", "name", "exchangeId", "marketType", "marketStartTime", "numberOfWinners") match {
        case Seq(
        JsString(id),
        JsString(name),
        JsString(exchangeId),
        JsString(marketType),
        JsString(marketStartTime),
        numberOfWinners: JsValue) =>
          Market(
            MarketId(id),
            MarketName(name),
            ExchangeId(exchangeId),
            MarketType(marketType),
            MarketStartTime(marketStartTime),
            readNumberOfWinners(numberOfWinners))
        case _ => deserializationError(s"Couldn't parse Market from '$json'")
      }

    private def readNumberOfWinners(json: JsValue): NumberOfWinners =
      json match {
        case JsNumber(n)  => NumberOfWinners(value = Some(n.intValue))
        case JsString("") => NumberOfWinners(value = None)
        case _            => deserializationError(s"Unable to read NumberOfWinners from `$json`")
      }

    override def write(obj: Market): JsValue =
      JsObject(
        ("type", JsString("MARKET")),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("exchangeId", JsString(obj.exchangeId.value)),
        ("marketType", JsString(obj.marketType.value)),
        ("marketStartTime", JsString(obj.marketStartTime.value)),
        ("numberOfWinners", writeNumberOfWinners(obj.numberOfWinners)))

    private def writeNumberOfWinners(numberOfWinners: NumberOfWinners): JsValue =
      numberOfWinners.value match {
        case Some(n) => JsNumber(n)
        case None    => JsString("")
      }
  }

  implicit val raceFormat: RootJsonFormat[Race] = new RootJsonFormat[Race] {
    override def read(json: JsValue): Race =
      json.asJsObject.getFields("id", "name", "countryCode", "venue", "startTime", "children") match {
        case Seq(
        JsString(id),
        JsString(name),
        JsString(countryCode),
        JsString(venue),
        JsString(startTime),
        JsArray(children)) =>
          Race(
            RaceId(id),
            RaceName(name),
            CountryCode(countryCode),
            Venue(venue),
            RaceStartTime(startTime),
            readChildren(children))
        case _ => deserializationError(s"Unable to read Race from '$json'")
      }

    private def readChildren(children: Vector[JsValue]): List[Market] =
      children.map(_.convertTo[Market]).toList

    override def write(obj: Race): JsValue =
      JsObject(
        ("type", JsString("RACE")),
        ("id", JsString(obj.id.value)),
        ("name", JsString(obj.name.value)),
        ("countryCode", JsString(obj.countryCode.value)),
        ("venue", JsString(obj.venue.value)),
        ("startTime", JsString(obj.startTime.value)),
        ("children", JsArray(obj.children.toJson)))
  }

  implicit val menuFormat: RootJsonFormat[Menu] = new RootJsonFormat[Menu] {
    override def read(json: JsValue): Menu =
      json.asJsObject.getFields("type", "id", "name", "children") match {
        case Seq(JsString("GROUP"), JsNumber(id), JsString("ROOT"), JsArray(children)) if id == 0 =>
          Menu(readChildren(children))
        case x =>
          deserializationError(
            s"""Expected '{"type": "GROUP","name": "ROOT","id": 0,"children": []}' but received '$x'""")
      }

    private def readChildren(children: Vector[JsValue]): List[EventType] =
      children.map(_.convertTo[EventType]).toList

    override def write(obj: Menu): JsValue =
      JsObject(
        ("type", JsString("GROUP")),
        ("name", JsString("ROOT")),
        ("id", JsNumber(0)),
        ("children", writeChildren(obj.children)))

    private def writeChildren(children: List[MenuItem]): JsValue = {
      JsArray(children.map {
        case e: EventType => e.toJson
        case x            => deserializationError(s"Menu can only have EventTypes as children - but found '$x'")
      }.toVector)
    }
  }
}
