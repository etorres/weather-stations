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
      .evalMap(updateStatsWith(_, statsRef))

  private def updateStatsWith(line: String, statsRef: Ref[IO, Map[String, Stats]]) =
    import scala.language.unsafeNulls
    line.split(";", 2).toList match
      case stationName :: measurement :: Nil =>
        val temperature = measurement.toDouble
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
      case _ =>
        IO.raiseError(
          IllegalArgumentException(
            s"""Expected line format: <string: station name>;<double: measurement>.
               | Instead found line: $line""".stripMargin.replaceAll("\\R", ""),
          ),
        )
