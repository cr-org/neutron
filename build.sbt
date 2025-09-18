import Dependencies.*
import Settings.*

ThisBuild / scalaVersion := supportedScala

lazy val `neutron-core` = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
          CompilerPlugins.betterMonadicFor,
          CompilerPlugins.kindProjector,
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.fs2,
          Libraries.newtype,
          Libraries.pulsar,
          Libraries.weaverCats % Test
        )
  )

lazy val `neutron-circe` = (project in file("circe"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`neutron-core`)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
          Libraries.avro4s % Provided,
          Libraries.circeCore,
          Libraries.circeParser
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
          Libraries.cats             % Test,
          Libraries.catsEffect       % Test,
          Libraries.cats             % Test,
          Libraries.weaverCats       % Test,
          Libraries.weaverScalaCheck % Test
        )
  )

lazy val tests = (project in file("tests"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    noPublish,
    Test / fork := true,
    Test / parallelExecution := false,
    libraryDependencies ++= List(
      CompilerPlugins.betterMonadicFor,
      CompilerPlugins.kindProjector,
      Libraries.avro4s       % Test,
      Libraries.circeCore    % Test,
      Libraries.circeGeneric % Test,
      Libraries.circeParser  % Test,
      Libraries.weaverCats   % Test
    )
  )
  .dependsOn(`neutron-circe`)

lazy val root = (project in file("."))
  .settings(name := "neutron")
  .settings(noPublish, publish / skip := true)
  .aggregate(
    `neutron-function`,
    `neutron-circe`,
    `neutron-core`,
    tests
  )
