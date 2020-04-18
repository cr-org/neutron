import mill._, scalalib._, scalafmt._, publish._
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.2`
import io.github.davidgregory084.TpolecatModule

val ScalaVersion = "2.13.1"

object Deps {
  object V {
    val BetterMonadicFor = "0.3.1"
    val Cats             = "2.1.1"
    val CatsEffect       = "2.1.2"
    val ContextApplied   = "0.1.2"
    val FS2              = "2.3.0"
    val KindProjector    = "0.11.0"
    val Newtype          = "0.4.3"
    val Pulsar           = "2.5.0"
    val ScalaCheck       = "1.14.0"
    val ScalaTest        = "3.1.0"
    val WartRemover      = "2.4.5"
  }

  val Cats         = ivy"org.typelevel::cats-core:${V.Cats}"
  val CatsEffect   = ivy"org.typelevel::cats-effect:${V.CatsEffect}"
  val FS2          = ivy"co.fs2::fs2-core:${V.FS2}"
  val Newtype      = ivy"io.estatico::newtype:${V.Newtype}"
  val PulsarClient = ivy"org.apache.pulsar:pulsar-client:${V.Pulsar}"

  // Testing
  val ScalaCheck              = ivy"org.scalacheck::scalacheck:${V.ScalaCheck}"
  val ScalaTest               = ivy"org.scalatest::scalatest:${V.ScalaTest}"
  val ScalaTestPlusScalaCheck = ivy"org.scalatestplus::scalacheck-1-14:3.1.0.0"

  // Other
  val BetterMonadicFor = ivy"com.olegpy::better-monadic-for:${V.BetterMonadicFor}"
  val ContextApplied   = ivy"org.augustjune::context-applied:${V.ContextApplied}"
  val KindProjector    = ivy"org.typelevel:::kind-projector:${V.KindProjector}"
  val WartRemover      = ivy"org.wartremover:::wartremover:${V.WartRemover}"
}

object core extends M with P {

  override def ivyDeps =
    Agg(
      Deps.Cats,
      Deps.CatsEffect,
      Deps.FS2,
      Deps.Newtype,
      Deps.PulsarClient
    )

  object test extends TestConfig

  def artifactName = "neutron-core"

}

trait P extends PublishModule {
  def publishVersion = "0.0.1"

  def pomSettings = PomSettings(
    description = "Fs2 bindings for Apache Pulsar",
    organization = "com.chatroulette",
    url = "http://gitlab.com/chatroulette/neutron",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.gitlab("chatroulette", "neutron"),
    developers = Seq(
      Developer("AndreasKostler", "Andreas Kostler","https://github.com/AndreasKostler"),
      Developer("gvolpe", "Gabriel Volpe","https://github.com/gvolpe")
    )
  )
}

trait M extends ScalaModule with ScalafmtModule with TpolecatModule {

  trait TestConfig extends Tests {
    override def ivyDeps =
      Agg(
        Deps.ScalaCheck,
        Deps.ScalaTest,
        Deps.ScalaTestPlusScalaCheck
      )

    override def testFrameworks =
      Seq("org.scalatest.tools.Framework")
  }

  def scalaVersion = ScalaVersion

  def compileIvyDeps =
    Agg(Deps.BetterMonadicFor, Deps.ContextApplied, Deps.KindProjector, Deps.WartRemover)

  def scalacPluginIvyDeps =
    Agg(Deps.BetterMonadicFor, Deps.ContextApplied, Deps.KindProjector, Deps.WartRemover)

  def downloadSources() = T.command {
    val wd = os.pwd / "lib-cache"
    os.makeDir.all(wd)
    resolveDeps(ivyDeps, sources = true)
      .map { dx =>
        dx.foreach { p =>
          val out = wd / p.path.last
          os.makeDir.all(out)
          os.proc("jar", "-xf", p.path).call(cwd = out)
        }
      }
  }

  val warts =
    List(
      "AsInstanceOf",
      "EitherProjectionPartial",
      "IsInstanceOf",
      "NonUnitStatements",
      "Null",
      "OptionPartial",
      "Product",
      "Return",
      "Serializable",
      "StringPlusAny",
      "Throw",
      "TraversableOps",
      "TryPartial",
      "Var"
    )

  def wartsScalacOptions =
    warts.map("-P:wartremover:traverser:org.wartremover.warts." + _)

  def scalacOptions = T { super.scalacOptions() :+ "-Ymacro-annotations" }

}
