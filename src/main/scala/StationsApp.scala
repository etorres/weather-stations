package es.eriktorr.weather

import domain.MeasurementsAnalyser

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.toTraverseOps
import fs2.io.file.Path

object StationsApp extends IOApp:
  override def run(args: List[String]): IO[ExitCode] = args match
    case measurementsPath :: maxRows :: parallelism :: Nil =>
      for
        _ <- IO.println(s"Reading measurements from: $measurementsPath")
        stats <- MeasurementsAnalyser.analyse(
          Path(measurementsPath),
          maxRows.toInt,
          parallelism.toInt,
        )
        _ <- stats.toList.sortBy { case (stationName, _) => stationName }.traverse {
          case (stationName, stats) =>
            IO.println(s"$stationName=${stats.min}/${stats.mean}/${stats.max}")
        }
        _ <- IO.println("Application completed")
      yield ExitCode.Success
    case _ => IO.raiseError(IllegalArgumentException("Usage: weather-stations filename"))
