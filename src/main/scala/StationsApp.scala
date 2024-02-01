package es.eriktorr.weather

import domain.{MeasurementsAnalyser, Stats}

import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.implicits.toTraverseOps
import fs2.io.file.Path

object StationsApp extends IOApp:
  override def run(args: List[String]): IO[ExitCode] = args match
    case measurementsPath :: Nil =>
      for
        _ <- IO.println(s"Reading measurements from: $measurementsPath")
        statsRef <- Ref.of[IO, Map[String, Stats]](Map.empty[String, Stats])
        _ <- MeasurementsAnalyser.analyse(Path(measurementsPath), statsRef).compile.drain
        finalStats <- statsRef.get
        _ <- finalStats.toList.sortBy { case (stationName, _) => stationName }.traverse {
          case (stationName, stats) =>
            IO.println(s"$stationName=${stats.min}/${stats.mean}/${stats.max}")
        }
        _ <- IO.println("Application completed")
      yield ExitCode.Success
    case _ => IO.raiseError(IllegalArgumentException("Usage: weather-stations filename"))
