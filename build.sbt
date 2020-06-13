import Dependencies._
import Settings._

lazy val `neutron-core` = project
  .settings(commonSettings)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    libraryDependencies ++= List(
      CompilerPlugins.betterMonadicFor,
      CompilerPlugins.contextApplied,
      CompilerPlugins.kindProjector,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.fs2,
      Libraries.newtype,
      Libraries.pulsar,
      Libraries.munitCore       % Test,
      Libraries.munitScalacheck % Test
    )
  )

lazy val root = (project in file("."))
  .settings(name := "neutron")
  .aggregate(
    `neutron-core`
  )
