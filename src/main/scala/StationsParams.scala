package es.eriktorr.weather

import cats.Show
import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import com.monovore.decline.{Argument, Opts}
import fs2.io.file.Path

final case class StationsParams(measurementsPath: Path)

object StationsParams:
  def opts: Opts[StationsParams] = Opts.argument[Path](metavar = "file").map(StationsParams.apply)

  given Argument[Path] = new Argument[Path]:
    override def read(string: String): ValidatedNel[String, Path] =
      if string.isBlank then "Non empty path expected".invalidNel else Path(string).validNel

    override def defaultMetavar: String = "path"

  given Show[StationsParams] =
    import scala.language.unsafeNulls
    Show.show(params => s"""{
                           |measurements-path: ${params.measurementsPath.toNioPath.toAbsolutePath}
                           |}""".stripMargin.replaceAll("\\R", ""))
