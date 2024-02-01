package es.eriktorr.weather
package infrastructure

import spec.StringGenerators.alphaNumericStringBetween

import org.scalacheck.Gen

import scala.math.BigDecimal.RoundingMode

object MeasurementGenerators:
  def stationNameGen: Gen[String] = alphaNumericStringBetween(5, 10)

  def temperatureGen: Gen[Double] =
    Gen.choose(-99.0d, 99.0d).map(x => BigDecimal(x).setScale(2, RoundingMode.HALF_UP).doubleValue)
