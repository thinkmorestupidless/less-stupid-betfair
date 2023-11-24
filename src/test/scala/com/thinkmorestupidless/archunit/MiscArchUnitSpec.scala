package com.thinkmorestupidless.archunit

import com.thinkmorestupidless.archunit.BaseArchUnitSpec.importedClasses
import com.thinkmorestupidless.utils.ScalaObjectUtils.ScalaObjectOps
import com.tngtech.archunit.core.domain.JavaCall.Predicates.target
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.scalatest.wordspec.AnyWordSpecLike

class MiscArchUnitSpec extends AnyWordSpecLike with BaseArchUnitSpec {

  "Classes" should {

    "never call EitherT.pure/rightT but call EitherT.safeRightT instead for race-condition safety" in {
      noClasses.should
        .callMethodWhere {
          target {
            owner {
              equivalentTo(cats.data.EitherT.getClass)
            }
          }.and(target {
            nameMatching("pure").or(nameMatching("rightT"))
          })
        }
        .because(
          s"EitherT.safeRightT (see ${com.thinkmorestupidless.utils.EitherTUtils.objectName}) should be used instead, " +
            s"since EitherT.pure/rightT does not protect against passing a Future as a parameter, " +
            s"which leaves the Future dangling and can lead to a race condition"
        )
        .check(importedClasses)
    }

    "never call println(...)" in {
      noClasses.should
        .callMethodWhere {
          target {
            owner {
              equivalentTo(scala.Predef.getClass)
            }
          }.and(target {
            nameMatching("println")
          })
        }
        .check(importedClasses)
    }
  }
}
