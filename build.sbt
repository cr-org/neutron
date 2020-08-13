import Dependencies._
import Settings._

lazy val `neutron-core` = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
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

lazy val `neutron-function` = (project in file("function"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
      Libraries.pulsarFunctionsApi,
      Libraries.java8Compat,
      Libraries.newtype,
      Libraries.cats            % Test,
      Libraries.catsEffect      % Test,
      Libraries.munitCore       % Test,
      Libraries.munitScalacheck % Test,
      Libraries.cats            % Test
    )
  )

lazy val root = (project in file("."))
  .settings(name := "neutron")
  .aggregate(
    `neutron-function`,
    `neutron-core`
  )
