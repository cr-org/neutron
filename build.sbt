import Dependencies._
import Settings._

scalaVersion in ThisBuild := "2.13.4"

def extraDeps(scalaVersion: String): List[ModuleID] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) =>
      List(
        CompilerPlugins.betterMonadicFor,
        CompilerPlugins.kindProjector,
        Libraries.newtype
      )
    case _ => Nil
  }

lazy val `neutron-core` = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(scalacOptions ++= compilerFlags(scalaVersion.value))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= List(
          Libraries.cats,
          Libraries.catsEffect,
          Libraries.fs2,
          Libraries.pulsar,
          Libraries.munitCore       % "it,test",
          Libraries.munitScalacheck % "it,test"
        ) ++ extraDeps(scalaVersion.value)
  )

lazy val `neutron-circe` = (project in file("circe"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`neutron-core`)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
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
          Libraries.cats            % Test,
          Libraries.catsEffect      % Test,
          Libraries.munitCore       % Test,
          Libraries.munitScalacheck % Test
        ) ++ extraDeps(scalaVersion.value)
  )

lazy val docs = (project in file("docs"))
  .dependsOn(`neutron-core`)
  .enablePlugins(ParadoxSitePlugin)
  .enablePlugins(ParadoxMaterialThemePlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MdocPlugin)
  .settings(
    noPublish,
    scmInfo := Some(
          ScmInfo(
            url("https://github.com/cr-org/neutron"),
            "scm:git:git@github.com:cr-org/neutron.git"
          )
        ),
    git.remoteRepo := scmInfo.value.get.connection.replace("scm:git:", ""),
    ghpagesNoJekyll := true,
    version := version.value.takeWhile(_ != '+'),
    paradoxProperties ++= Map(
          "scala-versions" -> (crossScalaVersions in `neutron-core`).value
                .map(CrossVersion.partialVersion)
                .flatten
                .map(_._2)
                .mkString("2.", "/", ""),
          "org" -> organization.value,
          "scala.binary.version" -> s"2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "neutron-core" -> s"${(`neutron-core` / name).value}_2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "neutron-circe" -> s"${(`neutron-circe` / name).value}_2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "neutron-function" -> s"${(`neutron-function` / name).value}_2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
          "version" -> version.value
        ),
    mdocIn := (Paradox / sourceDirectory).value,
    mdocExtraArguments ++= Seq("--no-link-hygiene"),
    makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
    ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox),
    paradoxMaterialTheme in Paradox := {
      ParadoxMaterialTheme()
        .withColor("red", "orange")
        .withLogoIcon("flash_on")
        .withCopyright("Copyright © ChatRoulette")
        .withRepository(uri("https://github.com/cr-org/neutron"))
    }
  )

lazy val root = (project in file("."))
  .settings(name := "neutron")
  .settings(noPublish)
  .aggregate(
    `neutron-function`,
    `neutron-circe`,
    `neutron-core`,
    docs
  )
