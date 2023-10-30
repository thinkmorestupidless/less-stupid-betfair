package com.thinkmorestupidless.extensions.slick

import com.github.tminglei.slickpg.{ExPostgresProfile, PgCirceJsonSupport, PgDate2Support}
import com.thinkmorestupidless.utils.Direction
import com.thinkmorestupidless.utils.Direction.{Ascending, Descending}
import slick.ast.TypedType
import slick.basic.Capability
import slick.jdbc.{JdbcCapabilities, JdbcType}
import slick.lifted.ColumnOrdered

import java.sql.ResultSet
import java.time.OffsetDateTime

trait CustomPostgresProfile extends ExPostgresProfile with PgCirceJsonSupport with PgDate2Support {
  override def pgjson = "jsonb"

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api = PostgresAPI

  object PostgresAPI extends API with JsonImplicits with DateTimeImplicits {
    override implicit val date2TzTimestampTypeMapper: JdbcType[OffsetDateTime] = CustomOffsetDateTimeMapper

    implicit class WithOrderingDirectionOps[T: TypedType](self: Rep[T]) {
      def withOrderingDirection(direction: Direction): ColumnOrdered[T] =
        direction match {
          case Ascending  => self.asc
          case Descending => self.desc
        }
    }
  }

  // https://github.com/tminglei/slick-pg/issues/493
  private object CustomOffsetDateTimeMapper
      extends GenericDateJdbcType[OffsetDateTime]("timestamptz", java.sql.Types.TIMESTAMP_WITH_TIMEZONE) {

    override def getValue(r: ResultSet, idx: Int): OffsetDateTime =
      classTag.runtimeClass match {
        case clazz if clazz == classOf[OffsetDateTime] =>
          r.getObject(idx, classOf[OffsetDateTime])
        case _ =>
          super.getValue(r, idx)
      }
  }
}

object CustomPostgresProfile extends CustomPostgresProfile
