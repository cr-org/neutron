import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.chatroulette"
ThisBuild / organizationName := "Chat Roulette"

lazy val root = (project in file("."))
  .settings(
    name := "neutron",
    scalacOptions += "-Ymacro-annotations",
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
    )
  )
