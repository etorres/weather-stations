package es.eriktorr.weather
package domain

import domain.Measurement.{StationName, Temperature}
import domain.MeasurementsAnalyserSuite.testCaseGen
import infrastructure.MeasurementGenerators.{stationNameGen, temperatureGen}

import cats.effect.{IO, Ref}
import cats.implicits.toTraverseOps
import fs2.io.file.Files
import fs2.{text, Stream}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits.genInstances
import org.scalacheck.effect.PropF.forAllF

import scala.util.Random

@SuppressWarnings(Array("org.wartremover.warts.Any"))
final class MeasurementsAnalyserSuite extends CatsEffectSuite with ScalaCheckEffectSuite:
  tempFileFixture.test("should compute temperature statistics by station"): tempFile =>
    forAllF(testCaseGen): testCase =>
      for
        _ <- Stream
          .emits(testCase.measurements)
          .map(measurement => s"${measurement.stationName};${measurement.temperature}")
          .intersperse("\n")
          .through(text.utf8.encode)
          .through(Files[IO].writeAll(tempFile))
          .compile
          .drain
        statsRef <- Ref.of[IO, Map[StationName, Stats]](Map.empty[StationName, Stats])
        _ <- MeasurementsAnalyser.analyse(tempFile, statsRef).compile.drain
        obtained <- statsRef.get
      yield assert(obtained == testCase.expected)

  private lazy val tempFileFixture = ResourceFixture(Files[IO].tempFile)

object MeasurementsAnalyserSuite:
  final private case class TestCase(
      measurements: List[Measurement],
      expected: Map[StationName, Stats],
  )

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  private val testCaseGen = for
    stationNames <- Gen.containerOfN[Set, StationName](7, stationNameGen)
    measurements <- stationNames.toList
      .flatTraverse(stationName =>
        for
          size <- Gen.choose(3, 7)
          temperatures <- Gen.containerOfN[List, Temperature](size, temperatureGen)
          measurements = temperatures.map(Measurement(stationName, _))
        yield measurements,
      )
      .map(Random.shuffle)
    expected = measurements.groupBy(_.stationName).map { case (stationName, xs) =>
      val temperatures: List[Double] = xs.map(_.temperature)
      stationName -> Stats(
        count = xs.size,
        min = temperatures.min,
        max = temperatures.max,
        sum = temperatures.sum,
      )
    }
  yield TestCase(measurements, expected)
