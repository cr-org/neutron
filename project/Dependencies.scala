import sbt._

object Dependencies {

  object Versions {
    val cats       = "2.1.0"
    val catsEffect = "2.1.2"
    val fs2        = "2.3.0"
    val newtype    = "0.4.3"
    val pulsar     = "2.5.0"

    val betterMonadicFor = "0.3.1"
    val contextApplied   = "0.1.2"
    val kindProjector    = "0.11.0"
  }

  object Libraries {
    val cats       = "org.typelevel"     %% "cats-core"    % Versions.cats
    val catsEffect = "org.typelevel"     %% "cats-effect"  % Versions.catsEffect
    val fs2        = "co.fs2"            %% "fs2-core"     % Versions.fs2
    val pulsar     = "org.apache.pulsar" % "pulsar-client" % Versions.pulsar
    val newtype    = "io.estatico"       %% "newtype"      % Versions.newtype
  }

  object CompilerPlugins {
    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % Versions.betterMonadicFor
    )
    val contextApplied = compilerPlugin(
      "org.augustjune" %% "context-applied" % Versions.contextApplied
    )
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full
    )
  }

}
