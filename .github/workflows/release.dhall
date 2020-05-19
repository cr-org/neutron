let GithubActions =
      https://raw.githubusercontent.com/regadas/github-actions-dhall/master/package.dhall sha256:b42b062af139587666185c6fb72cc2994aa85a30065324174760b7d29a9d81c9

-- TODO: These 3 should eventually be defined upstream
let GpgSetup   = ./gpg-setup.dhall
let ScalaSetup = ./scala-setup.dhall
let CiRelease  = ./sbt-ci-release.dhall

let setup =
      [ GithubActions.steps.checkout
      , ScalaSetup
      , GpgSetup
      , CiRelease
          { ref = "\${{ github.ref }}"
          , pgpPassphrase = "\${{ secrets.PGP_PASSPHRASE }}"
          , pgpSecret = "\${{ secrets.PGP_SECRET }}"
          , sonatypePassword = "\${{ secrets.SONATYPE_PASSWORD }}"
          , sonatypeUsername = "\${{ secrets.SONATYPE_USERNAME }}"
          }
      ]

in  GithubActions.Workflow::{
    , name = "Release"
    , on = GithubActions.On::{
      , push = Some GithubActions.Push::{
          , branches = Some [ "master" ]
          , tags = Some ["*"]
        }
      }
    , jobs = toMap
        { build = GithubActions.Job::{
          , name = "Publish"
          , needs = None (List Text)
          , runs-on = GithubActions.types.RunsOn.`ubuntu-18.04`
          , steps = setup
          }
        }
    }
