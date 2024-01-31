package es.eriktorr.weather
package data.refined

import io.github.iltotore.iron.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.numeric.{GreaterEqual, LessEqual}
import io.github.iltotore.iron.constraint.string.Blank

object Types:
  type Between[Min, Max] = GreaterEqual[Min] & LessEqual[Max]

  type NonEmptyString =
    DescribedAs[Not[Blank], "Should contain at least one non-whitespace character"]
