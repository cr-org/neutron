import sbt._

object Dependencies {

  object V {
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
    val cats       = "org.typelevel"     %% "cats-core"    % V.cats
    val catsEffect = "org.typelevel"     %% "cats-effect"  % V.catsEffect
    val fs2        = "co.fs2"            %% "fs2-core"     % V.fs2
    val pulsar     = "org.apache.pulsar" % "pulsar-client" % V.pulsar
    val newtype    = "io.estatico"       %% "newtype"      % V.newtype
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
  }

}
