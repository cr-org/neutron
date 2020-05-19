import Dependencies._

ThisBuild / crossScalaVersions := Seq("2.12.10", "2.13.2")
ThisBuild / organization := "com.chatroulette"
ThisBuild / organizationName := "Chatroulette"

def compilerFlags(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => List("-Ymacro-annotations")
    case _             => List.empty
  }

def macroParadisePlugin(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => List.empty
    case _             => List(CompilerPlugins.macroParadise)
  }

inThisBuild(
  List(
    organization := "com.chatroulette",
    homepage := Some(url("https://github.com/cr-org/neutron")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List()
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "neutron",
    scalacOptions ++= compilerFlags(scalaVersion.value),
    scalafmtOnCompile := true,
    libraryDependencies ++= List(
          CompilerPlugins.betterMonadicFor,
          CompilerPlugins.contextApplied,
          CompilerPlugins.kindProjector,
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.fs2,
          Libraries.newtype,
          Libraries.pulsar
        ),
    libraryDependencies ++= macroParadisePlugin(scalaVersion.value)
  )
