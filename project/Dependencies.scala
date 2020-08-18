import sbt._

object Dependencies {

  object V {
    val java8Compat = "0.9.1"

    val cats       = "2.1.1"
    val catsEffect = "2.1.4"
    val fs2        = "2.4.3"
    val munit      = "0.7.11"
    val newtype    = "0.4.4"
    val pulsar     = "2.5.2"

    val betterMonadicFor = "0.3.1"
    val contextApplied   = "0.1.4"
    val kindProjector    = "0.11.0"
    val macroParadise    = "2.1.1"
  }

  object Libraries {
    val cats       = "org.typelevel"     %% "cats-core"    % V.cats
    val catsEffect = "org.typelevel"     %% "cats-effect"  % V.catsEffect
    val fs2        = "co.fs2"            %% "fs2-core"     % V.fs2
    val newtype    = "io.estatico"       %% "newtype"      % V.newtype

    val java8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % V.java8Compat

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
