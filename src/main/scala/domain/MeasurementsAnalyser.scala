package es.eriktorr.weather
package domain

import cats.effect.{IO, Ref}
import cats.implicits.toTraverseOps
import fs2.io.file.{Files, Path}
import fs2.Stream

object MeasurementsAnalyser:
  def analyse(
      measurementsPath: Path,
      maxRows: Int = 1_000_000_000,
      parallelism: Int = 2,
  ): IO[Map[String, Stats]] =
    for
      processorToStatsRef <- Range(0, parallelism).toList
        .traverse(processor =>
          Ref.of[IO, Map[String, Stats]](Map.empty[String, Stats]).map(processor -> _),
        )
        .map(_.toMap)
      _ <- Stream
        .range(0, parallelism)
        .map(processor =>
          Files[IO]
            .readUtf8Lines(measurementsPath)
            .take(maxRows)
            .filter(_.charAt(0).toInt % parallelism == processor)
            .evalMap(updateStatsWith(_, processorToStatsRef(processor))),
        )
        .parJoin(parallelism)
        .compile
        .toList
      statsByProcessor <- processorToStatsRef.values.toList.traverse(_.get)
      allStats = statsByProcessor.fold(Map.empty) { case (x, y) =>
        x ++ y.toList.map { case (stationName, stats) =>
          x.get(stationName) match
            case Some(other) =>
              val updatedStats =
                Stats(
                  count = stats.count + other.count,
                  min = Math.min(stats.min, other.min),
                  max = Math.max(stats.max, other.max),
                  sum = stats.sum + other.sum,
                )
              stationName -> updatedStats
            case None => stationName -> stats
        }.toMap
      }
    yield allStats

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
