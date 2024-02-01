package es.eriktorr.weather
package domain

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
          .map { case (stationName, temperature) => s"$stationName;$temperature" }
          .intersperse("\n")
          .through(text.utf8.encode)
          .through(Files[IO].writeAll(tempFile))
          .compile
          .drain
        statsRef <- Ref.of[IO, Map[String, Stats]](Map.empty[String, Stats])
        _ <- MeasurementsAnalyser.analyse(tempFile, statsRef).compile.drain
        obtained <- statsRef.get
      yield assert(obtained == testCase.expected)

  private lazy val tempFileFixture = ResourceFixture(Files[IO].tempFile)

object MeasurementsAnalyserSuite:
  final private case class TestCase(
      measurements: List[(String, Double)],
      expected: Map[String, Stats],
  )

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  private val testCaseGen = for
    stationNames <- Gen.containerOfN[Set, String](7, stationNameGen)
    measurements <- stationNames.toList
      .flatTraverse(stationName =>
        for
          size <- Gen.choose(3, 7)
          temperatures <- Gen.containerOfN[List, Double](size, temperatureGen)
          measurements = temperatures.map((stationName, _))
        yield measurements,
      )
      .map(Random.shuffle)
    expected = measurements.groupBy { case (stationName, _) => stationName }.map {
      case (stationName, xs) =>
        val temperatures: List[Double] = xs.map { case (_, temperature) => temperature }
        stationName -> Stats(
          count = xs.size,
          min = temperatures.min,
          max = temperatures.max,
          sum = temperatures.sum,
        )
    }
  yield TestCase(measurements, expected)
