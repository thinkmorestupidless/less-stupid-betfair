package com.thinkmorestupidless.archunit

import com.tngtech.archunit.core.domain.{JavaClass, JavaClasses, JavaModifier}
import com.tngtech.archunit.core.importer.{ClassFileImporter, Location}

import java.lang.annotation.Annotation
import java.util.regex.Pattern
import scala.reflect.ClassTag

trait BaseArchUnitSpec {
  implicit class JavaClassOps(self: JavaClass) {
    def isConcrete: Boolean =
      !(self.isInterface || self.getModifiers.contains(JavaModifier.ABSTRACT))

    def hasAnnotation[T <: Annotation](implicit classTag: ClassTag[T]): Boolean =
      self.isAnnotatedWith(classTag.runtimeClass.asInstanceOf[Class[_ <: Annotation]])
  }
}

object BaseArchUnitSpec {

  private val classFileImporter = new ClassFileImporter()
  lazy val importedClasses: JavaClasses = classFileImporter.importPackages("com.thinkmorestupidless")
  lazy val importedProductionClasses: JavaClasses = classFileImporter
    .withImportOption { (location: Location) =>
      !location.matches(Pattern.compile(".*/target/scala-.*/test-classes/.*"))
    }
    .importPackages("com.thinkmorestupidless")
}
