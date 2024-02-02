package es.eriktorr.weather
package domain

import data.validated.ValidatedNecExtensions.validatedNecTo
import domain.Measurement.{StationName, Temperature}

import cats.effect.{IO, Ref}
import cats.implicits.catsSyntaxTuple2Semigroupal
import fs2.Stream
import fs2.io.file.{Files, Path}

object MeasurementsAnalyser:
  def analyse(
      measurementsPath: Path,
      statsRef: Ref[IO, Map[StationName, Stats]],
      maxRows: Long = 1_000_000_000L,
  ): Stream[IO, Unit] =
    Files[IO]
      .readUtf8Lines(measurementsPath)
      .take(maxRows)
      .flatMap(measurementFrom)
      .evalMap(temperatureStats(_, statsRef))

  private def measurementFrom(line: String): Stream[IO, Measurement] =
    import scala.language.unsafeNulls
    line.split(";", 2).toList match
      case stationName :: temperature :: Nil =>
        Stream.eval(
          (StationName.from(stationName), Temperature.from(temperature))
            .mapN(Measurement.apply)
            .validated,
        )
      case _ =>
        Stream.raiseError(
          IllegalArgumentException(
            s"""Expected line format: <string: station name>;<double: measurement>.
               | Instead found line: $line""".stripMargin.replaceAll("\\R", ""),
          ),
        )

  private def temperatureStats(
      measurement: Measurement,
      statsRef: Ref[IO, Map[StationName, Stats]],
  ): IO[Unit] =
    for
      stationNameToStats <- statsRef.get
      currentStats = stationNameToStats.getOrElse(
        measurement.stationName,
        Stats(0L, measurement.temperature, measurement.temperature, 0.0d),
      )
      _ <- statsRef.update(
        _ + (measurement.stationName -> Stats(
          count = currentStats.count + 1L,
          min = Math.min(currentStats.min, measurement.temperature),
          max = Math.max(currentStats.max, measurement.temperature),
          sum = currentStats.sum + measurement.temperature,
        )),
      )
    yield ()
