package com.thinkmorestupidless.utils

object OptionalSetUtils {

  implicit class OptionSetOps[A](self: Option[Set[A]]) {
    def +(that: Option[Set[A]]): Option[Set[A]] =
      for {
        left <- self
        right <- that
      } yield left ++ right

    def diff(that: Option[Set[A]]): Option[Set[A]] =
      for {
        left <- self
        right <- that
      } yield left.diff(right)

    def intersect(that: Option[Set[A]]): Option[Set[A]] =
      for {
        left <- self
        right <- that
      } yield left.intersect(right)
  }
}
