package es.eriktorr.weather
package domain

import cats.effect.{IO, Ref}
import fs2.Stream
import fs2.io.file.{Files, Path}

object MeasurementsAnalyser:
  def analyse(
      measurementsPath: Path,
      statsRef: Ref[IO, Map[String, Stats]],
      maxRows: Long = 1_000_000_000L,
  ): Stream[IO, Unit] =
    Files[IO]
      .readUtf8Lines(measurementsPath)
      .take(maxRows)
      .flatMap(measurementFrom)
      .evalMap(temperatureStats(_, statsRef))

  private def measurementFrom(line: String): Stream[IO, (String, Double)] =
    import scala.language.unsafeNulls
    line.split(";", 2).toList match
      case stationName :: temperature :: Nil => Stream.emit((stationName, temperature.toDouble))
      case _ =>
        Stream.raiseError(
          IllegalArgumentException(
            s"""Expected line format: <string: station name>;<double: measurement>.
               | Instead found line: $line""".stripMargin.replaceAll("\\R", ""),
          ),
        )

  private def temperatureStats(
      measurement: (String, Double),
      statsRef: Ref[IO, Map[String, Stats]],
  ): IO[Unit] =
    val (stationName, temperature) = measurement
    for
      stationNameToStats <- statsRef.get
      currentStats = stationNameToStats.getOrElse(
        stationName,
        Stats(0L, temperature, temperature, 0.0d),
      )
      _ <- statsRef.update(
        _ + (stationName -> Stats(
          count = currentStats.count + 1L,
          min = Math.min(currentStats.min, temperature),
          max = Math.max(currentStats.max, temperature),
          sum = currentStats.sum + temperature,
        )),
      )
    yield ()
