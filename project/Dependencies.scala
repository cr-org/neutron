import sbt._

object Dependencies {

  object V {
    val java8Compat = "1.0.2"

    val avro4s = "4.1.2"

    val cats       = "2.13.0"
    val catsEffect = "3.6.1"
    val fs2        = "3.11.0"
    val circe      = "0.14.10"

    val newtype = "0.4.4"
    val pulsar  = "4.0.3"
    val weaver  = "0.8.4"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.3"
    val macroParadise    = "2.1.1"
  }

  object Libraries {
    val cats       = "org.typelevel" %% "cats-core"   % V.cats
    val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
    val fs2        = "co.fs2"        %% "fs2-core"    % V.fs2
    val newtype    = "io.estatico"   %% "newtype"     % V.newtype

    val circeCore    = "io.circe" %% "circe-core"    % V.circe
    val circeGeneric = "io.circe" %% "circe-generic" % V.circe
    val circeParser  = "io.circe" %% "circe-parser"  % V.circe

    val avro4s = "com.sksamuel.avro4s" %% "avro4s-core" % V.avro4s

    val java8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % V.java8Compat

    val pulsar             = "org.apache.pulsar" % "pulsar-client"        % V.pulsar
    val pulsarFunctionsApi = "org.apache.pulsar" % "pulsar-functions-api" % V.pulsar

    // Testing
    val weaverCats       = "com.disneystreaming" %% "weaver-cats"       % V.weaver
    val weaverScalaCheck = "com.disneystreaming" %% "weaver-scalacheck" % V.weaver
  }

  object CompilerPlugins {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
    )
    val macroParadise = compilerPlugin(
      "org.scalamacros" % "paradise" % V.macroParadise cross CrossVersion.full
    )
  }

}
