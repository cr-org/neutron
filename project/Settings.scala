import sbt._
import sbt.Keys._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import Dependencies.CompilerPlugins

object Settings {
  val commonSettings = Seq(
    scalacOptions ++= compilerFlags(scalaVersion.value),
    scalafmtOnCompile := true,
    autoAPIMappings := true,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= macroParadisePlugin(scalaVersion.value),
    ThisBuild / homepage := Some(url("https://github.com/cr-org/neutron")),
    ThisBuild / organization := "com.chatroulette",
    ThisBuild / organizationName := "Chatroulette",
    ThisBuild / startYear := Some(2020),
    ThisBuild / licenses := List(
          "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
        ),
    ThisBuild / developers := List(
          Developer(
            "agjacome",
            "Alberto G. Jácome",
            "alberto.jacome@chatroulette.com",
            url("https://github.com/agjacome")
          ),
          Developer(
            "AndreasKostler",
            "Andreas Kostler",
            "andreas.kostler@chatroulette.com",
            url("https://github.com/AndreasKostler")
          ),
          Developer(
            "gvolpe",
            "Gabriel Volpe",
            "gabriel.volpe@chatroulette.com",
            url("https://gvolpe.github.io")
          ),
          Developer(
            "psisoyev",
            "Pavels Sisojevs",
            "pavels.sisojevs@chatroulette.com",
            url("https://scala.monster/")
          ),
          Developer(
            "tabdulazim",
            "Tamer Abdulazim",
            "tamer.abdulazim@chatroulette.com",
            url("https://github.com/tabdulazim")
          )
        )
  )

  val noPublish = {
    skip in publish := true
  }

  def compilerFlags(v: String) =
    CrossVersion.partialVersion(v) match {
      case Some((2, 13)) => List("-Ymacro-annotations")
      case _             => List.empty
    }

  def macroParadisePlugin(v: String) =
    CrossVersion.partialVersion(v) match {
      case Some((2, 13)) => List.empty
      case _             => List(CompilerPlugins.macroParadise)
    }
}
