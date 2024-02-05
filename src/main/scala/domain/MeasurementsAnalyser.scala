package es.eriktorr.weather
package domain

import cats.effect.{IO, Ref}
import fs2.io.file.{Files, Path}

object MeasurementsAnalyser:
  def analyse(measurementsPath: Path): IO[Map[String, Stats]] =
    for
      statsRef <- Ref.of[IO, Map[String, Stats]](Map.empty[String, Stats])
      _ <- Files[IO]
        .readUtf8Lines(measurementsPath)
        .filter(_.trim.nn.nonEmpty)
        .evalMap(updateStatsWith(_, statsRef))
        .compile
        .toList
      stats <- statsRef.get
    yield stats

  private def updateStatsWith(line: String, statsRef: Ref[IO, Map[String, Stats]]) =
    import scala.language.unsafeNulls
    line.split(";", 2).toList match
      case stationName :: measurement :: Nil =>
        val temperature = measurement.toDouble
        statsRef.update { stationNameToStats =>
          val currentStats = stationNameToStats.getOrElse(
            stationName,
            Stats(0L, temperature, temperature, 0.0d),
          )
          stationNameToStats + (stationName -> Stats(
            count = currentStats.count + 1L,
            min = Math.min(currentStats.min, temperature),
            max = Math.max(currentStats.max, temperature),
            sum = currentStats.sum + temperature,
          ))
        }
      case _ =>
        IO.raiseError(
          IllegalArgumentException(
            s"""Expected line format: <string: station name>;<double: measurement>.
               | Instead found line: $line""".stripMargin.replaceAll("\\R", ""),
          ),
        )
