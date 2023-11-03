package com.thinkmorestupidless.archunit

import com.thinkmorestupidless.archunit.BaseArchUnitSpec.importedClasses
import com.tngtech.archunit.core.domain.JavaCall.Predicates.target
import com.tngtech.archunit.core.domain.JavaClass.Predicates.{`type`, equivalentTo}
import com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.scalatest.wordspec.AnyWordSpecLike

class DateTimeTypesArchUnitSpec extends AnyWordSpecLike with BaseArchUnitSpec {

  "Classes" should {

    "never access DateTimeFormatter.(ISO_LOCAL_DATE_TIME|ISO_LOCAL_TIME)" in {
      noClasses.that
        .haveNameNotMatching("^phoenix\\.core\\.TimeUtils\\$.*$")
        .should
        .accessField(classOf[java.time.format.DateTimeFormatter], "ISO_LOCAL_DATE_TIME")
        .orShould
        .accessField(classOf[java.time.format.DateTimeFormatter], "ISO_LOCAL_TIME")
        .because(
          "local (date) times are inherently unsafe " +
            "since they give the impression of pointing to a specific moment in time, " +
            "while in fact they do not - " +
            "the actual moment in time depends on what time zone is assumed further down the code"
        )
        .check(importedClasses)
    }

    "never call (Instant|LocalDate|OffsetDateTime).now(...) but use Clock instead" in {
      noClasses.that
        .areNotAssignableTo(classOf[java.time.Clock])
        .should
        .callMethodWhere {
          target {
            owner {
              equivalentTo(classOf[java.time.Instant])
                .or(equivalentTo(classOf[java.time.LocalDate]))
                .or(equivalentTo(classOf[java.time.OffsetDateTime]))
            }
          }.and(target {
            nameMatching("now")
          })
        }
        .because(s"methods of ${classOf[java.time.Clock].getName} should be called instead")
        .check(importedClasses)
    }

    "never depend on LocalTime or LocalDateTime" in {
      noClasses.that
        .haveNameNotMatching("^phoenix.core.TimeUtils.*")
        .and()
        .haveNameNotMatching("^phoenix.core.config.CustomConfigReaders.*")
        .should
        .dependOnClassesThat
        .areAssignableTo {
          `type`(classOf[java.time.LocalDateTime]).or(`type`(classOf[java.time.LocalTime]))
        }
        .because(
          "LocalDateTime and LocalTime are inherently unsafe " +
            "since they give the impression of pointing to a specific moment in time, " +
            "while in fact they do not - " +
            "the actual moment in time depends on what time zone is assumed further down the code"
        )
        .check(importedClasses)
    }

    "never depend on ZonedDateTime but use OffsetDateTime instead" in {
      noClasses.should.dependOnClassesThat
        .areAssignableTo(classOf[java.time.ZonedDateTime])
        .orShould
        .accessField(classOf[java.time.format.DateTimeFormatter], "ISO_ZONED_DATE_TIME")
        .because(
          "ZonedDateTime is discouraged (albeit not harmful) since it keeps both ZoneOffset and ZoneId; " +
            "the latter is redundant, time zone info is lost anyway when storing TIMESTAMPTZs in Postgres; " +
            "use OffsetDateTime (which keeps just ZoneOffset) instead"
        )
        .check(importedClasses)
    }
  }
}
