import sbt._

object Dependencies {

  object Versions {
    val cats        = "2.1.0"
    val catsEffect  = "2.1.2"
    val catsMeowMtl = "0.4.0"
    val circe       = "0.13.0"
    val fs2         = "2.3.0"
    val http4s      = "0.21.1"
    val log4cats    = "1.0.0"
    val logback     = "1.2.1"
    val newtype     = "0.4.3"
    val pulsar      = "2.5.0"
    val refined     = "0.9.12"

    val betterMonadicFor = "0.3.1"
    val contextApplied   = "0.1.2"
    val kindProjector    = "0.11.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeParser  = circe("circe-parser")
    val circeRefined = circe("circe-refined")

    val fs2 = "co.fs2" %% "fs2-core" % Versions.fs2

    val http4sDsl    = http4s("http4s-dsl")
    val http4sServer = http4s("http4s-blaze-server")
    val http4sClient = http4s("http4s-blaze-client")
    val http4sCirce  = http4s("http4s-circe")

    val pulsar = "org.apache.pulsar" % "pulsar-client" % Versions.pulsar

    val refinedCore = "eu.timepit" %% "refined"      % Versions.refined
    val refinedCats = "eu.timepit" %% "refined-cats" % Versions.refined

    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % Versions.log4cats
    val newtype  = "io.estatico"       %% "newtype"        % Versions.newtype

    // Runtime
    val logback = "ch.qos.logback" % "logback-classic" % Versions.logback
  }

  object CompilerPlugins {
    val betterMonadicFor = compilerPlugin("com.olegpy"     %% "better-monadic-for" % Versions.betterMonadicFor)
    val contextApplied   = compilerPlugin("org.augustjune" %% "context-applied"    % Versions.contextApplied)
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full
    )
  }

}
