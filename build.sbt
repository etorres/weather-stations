ThisBuild / organization := "es.eriktorr"
ThisBuild / version := "1.0.0"
ThisBuild / idePackagePrefix := Some("es.eriktorr.weather")
Global / excludeLintKeys += idePackagePrefix

ThisBuild / scalaVersion := "3.3.1"

ThisBuild / scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-source:future", // https://github.com/oleg-py/better-monadic-for
  "-Yexplicit-nulls", // https://docs.scala-lang.org/scala3/reference/other-new-features/explicit-nulls.html
  "-Ysafe-init", // https://docs.scala-lang.org/scala3/reference/other-new-features/safe-initialization.html
  "-Wnonunit-statement",
  "-Wunused:all",
)

Global / cancelable := true
Global / fork := true
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / semanticdbEnabled := true
ThisBuild / javacOptions ++= Seq("-source", "21", "-target", "21")

lazy val MUnitFramework = new TestFramework("munit.Framework")
lazy val warts = Warts.unsafe.filter(_ != Wart.DefaultArguments)

Compile / doc / sources := Seq()
Compile / compile / wartremoverErrors ++= warts
Test / compile / wartremoverErrors ++= warts
Test / testFrameworks += MUnitFramework
Test / testOptions += Tests.Argument(MUnitFramework, "--exclude-tags=online")

addCommandAlias(
  "check",
  "; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; scalafixAll; scalafmtSbtCheck; scalafmtCheckAll",
)

lazy val root = (project in file("."))
  .settings(
    name := "weather-stations",
    Universal / maintainer := "https://eriktorr.es",
    Compile / mainClass := Some("es.eriktorr.weather.StationsApp"),
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.9.4",
      "co.fs2" %% "fs2-io" % "3.9.4",
      "com.lihaoyi" %% "os-lib" % "0.9.3" % Test,
      "com.monovore" %% "decline" % "2.4.1",
      "com.monovore" %% "decline-effect" % "2.4.1",
      "io.chrisdavenport" %% "cats-scalacheck" % "0.3.2" % Test,
      "io.circe" %% "circe-core" % "0.14.5",
      "io.github.iltotore" %% "iron" % "2.4.0",
      "io.github.iltotore" %% "iron-cats" % "2.4.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.3",
      "org.typelevel" %% "cats-effect-kernel" % "3.5.3",
      "org.typelevel" %% "cats-effect-std" % "3.5.3",
      "org.typelevel" %% "cats-kernel" % "2.10.0",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
      "org.typelevel" %% "scalacheck-effect" % "1.0.4" % Test,
      "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test,
    ),
    onLoadMessage := {
      s"""Custom tasks:
         |check - run all project checks
         |""".stripMargin
    },
  )
  .enablePlugins(JavaAppPackaging)
