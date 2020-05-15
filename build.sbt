import Dependencies._

ThisBuild / crossScalaVersions := Seq("2.12.10", "2.13.2")
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.chatroulette"
ThisBuild / organizationName := "Chat Roulette"

def compilerFlags(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => Seq("-Ymacro-annotations")
    case _             => Seq.empty
  }

def macroParadisePlugin(v: String) =
  CrossVersion.partialVersion(v) match {
    case Some((2, 13)) => Seq.empty
    case _             => Seq(CompilerPlugins.macroParadise)
  }

lazy val root = (project in file("."))
  .settings(
    name := "neutron",
    scalacOptions ++= compilerFlags(scalaVersion.value),
    scalafmtOnCompile := true,
    libraryDependencies ++= Seq(
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
