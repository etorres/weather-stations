package es.eriktorr.weather

import domain.Measurement.StationName
import domain.{MeasurementsAnalyser, Stats}

import cats.effect.std.Console
import cats.effect.{ExitCode, IO, Ref}
import cats.implicits.{showInterpolator, toTraverseOps}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object StationsApp extends CommandIOApp(name = "weather-stations", header = "Weather Stations"):
  override def main: Opts[IO[ExitCode]] = StationsParams.opts.map(params =>
    for
      _ <- Console[IO].println(show"Starting application with parameters : $params")
      statsRef <- Ref.of[IO, Map[StationName, Stats]](Map.empty[StationName, Stats])
      _ <- MeasurementsAnalyser.analyse(params.measurementsPath, statsRef).compile.drain
      finalStats <- statsRef.get
      _ <- finalStats.toList.sortBy { case (stationName, _) => stationName }.traverse {
        case (stationName, stats) =>
          Console[IO].println(s"$stationName=${stats.min}/${stats.mean}/${stats.max}")
      }
      _ <- Console[IO].println("Application completed")
    yield ExitCode.Success,
  )
