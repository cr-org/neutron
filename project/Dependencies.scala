import sbt._

object Dependencies {

  object V {
    val cats       = "2.3.1"
    val catsEffect = "2.3.1"
    val circe      = "0.14.0-M2"
    val fs2        = "2.5.0"
    val munit      = "0.7.20"
    val newtype    = "0.4.4"
    val pulsar     = "2.6.2"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.11.3"
  }

  object Libraries {
    val cats       = "org.typelevel" %% "cats-core"   % V.cats
    val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
    val fs2        = "co.fs2"        %% "fs2-core"    % V.fs2
    val newtype    = "io.estatico"   %% "newtype"     % V.newtype

    val circeCore   = "io.circe" %% "circe-core"   % V.circe
    val circeParser = "io.circe" %% "circe-parser" % V.circe

    val pulsar             = "org.apache.pulsar" % "pulsar-client"        % V.pulsar
    val pulsarFunctionsApi = "org.apache.pulsar" % "pulsar-functions-api" % V.pulsar

    // Testing
    val munitCore       = "org.scalameta" %% "munit"            % V.munit
    val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % V.munit
  }

  object CompilerPlugins {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
    )
  }

}
