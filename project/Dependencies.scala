import sbt._

object Dependencies {

  object V {
    val java8Compat = "1.0.0"

    val avro4s     = "4.0.9"
    val cats       = "2.6.1"
    val catsEffect = "2.5.1"
    val circe      = "0.14.0"
    val fs2        = "2.5.6"
    val newtype    = "0.4.4"
    val pulsar     = "2.7.2"
    val weaver     = "0.6.3"

    val betterMonadicFor = "0.3.1"
    val contextApplied   = "0.1.4"
    val kindProjector    = "0.13.0"
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
    val contextApplied = compilerPlugin(
      "org.augustjune" %% "context-applied" % V.contextApplied
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
    )
    val macroParadise = compilerPlugin(
      "org.scalamacros" % "paradise" % V.macroParadise cross CrossVersion.full
    )
  }

}
