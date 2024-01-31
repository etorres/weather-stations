package es.eriktorr.weather

import StationsParamsSuite.inputPathGen
import spec.StringGenerators.alphaNumericStringBetween

import cats.implicits.catsSyntaxEitherId
import com.monovore.decline.{Command, Help}
import fs2.io.file.Path
import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

final class StationsParamsSuite extends ScalaCheckSuite:
  property("should load application parameters"):
    forAll(inputPathGen)(inputPath =>
      Command(name = "name", header = "header")(StationsParams.opts)
        .parse(List(inputPath)) == StationsParams(Path(inputPath)).asRight[Help],
    )

object StationsParamsSuite:
  private val inputPathGen = alphaNumericStringBetween(3, 12)
