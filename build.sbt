import Dependencies._
import Settings._

lazy val `neutron-core` = (project in file("core"))
  .settings(commonSettings)
  .enablePlugins(AutomateHeaderPlugin)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= List(
      CompilerPlugins.betterMonadicFor,
      CompilerPlugins.contextApplied,
      CompilerPlugins.kindProjector,
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.fs2,
      Libraries.newtype,
      Libraries.pulsar,
      Libraries.munitCore       % "it,test",
      Libraries.munitScalacheck % "it,test"
    )
  )

lazy val root = (project in file("."))
  .settings(name := "neutron")
  .aggregate(
    `neutron-core`
  )
