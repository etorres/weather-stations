package es.eriktorr.weather
package infrastructure

import domain.Measurement.{StationName, Temperature}

import es.eriktorr.weather.spec.StringGenerators.alphaNumericStringBetween
import org.scalacheck.Gen

object MeasurementGenerators:
  def stationNameGen: Gen[StationName] =
    alphaNumericStringBetween(5, 10).map(StationName.unsafeFrom)

  def temperatureGen: Gen[Temperature] = Gen.choose(-99.0d, 99.0d).map(Temperature.unsafeFrom)
