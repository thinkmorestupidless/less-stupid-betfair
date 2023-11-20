package com.thinkmorestupidless.utils

object OptionalListUtils {

  implicit class OptionListOps[A](self: Option[List[A]]) {
    def +(that: Option[List[A]]): Option[List[A]] =
      for {
        left <- self
        right <- that
      } yield left ++ right

    def diff(that: Option[List[A]]): Option[List[A]] =
      for {
        left <- self
        right <- that
      } yield left.diff(right)

    def intersect(that: Option[List[A]]): Option[List[A]] =
      for {
        left <- self
        right <- that
      } yield left.intersect(right)
  }
}
