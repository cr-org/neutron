let GithubActions =
      https://raw.githubusercontent.com/regadas/github-actions-dhall/master/package.dhall sha256:8efe6772e27f99ed3a9201b4e45c68eeaaf82c349e70d36fbe89185a324f6519

let matrix = toMap { java = [ "8.0.242", "11.0.5" ] }

let setup =
      [ GithubActions.steps.checkout
      , GithubActions.steps.run { run = "docker-compose up -d" }
        // { name = Some "Starting up Pulsar 🐳" }
      , GithubActions.steps.run
          { run =
              ''
              shasum build.sbt \
                project/plugins.sbt \
                project/build.properties \
                project/Dependencies.scala > gha.cache.tmp
              ''
          }
      , GithubActions.steps.cache
          { path = "~/.sbt", key = "sbt", hashFile = "gha.cache.tmp" }
      , GithubActions.steps.cache
          { path = "~/.cache/coursier"
          , key = "coursier"
          , hashFile = "gha.cache.tmp"
          }
      , GithubActions.steps.olafurpg/java-setup
          { java-version = "\${{ matrix.java}}" }
      , GithubActions.steps.run { run = "sbt \"++ test\"" }
      , GithubActions.steps.run { run = "sbt \"++ it:test\"" }
      , GithubActions.steps.run { run = "docker-compose down" }
        // { name = Some "Shutting down Pulsar 🐳" }
      ]

in  GithubActions.Workflow::{
    , name = "Scala"
    , on = GithubActions.On::{
      , push = Some GithubActions.Push::{ branches = Some [ "master" ] }
      , pull_request = Some GithubActions.PullRequest::{=}
      }
    , jobs = toMap
        { build = GithubActions.Job::{
          , name = Some "Build"
          , needs = None (List Text)
          , strategy = Some GithubActions.Strategy::{ matrix = matrix }
          , runs-on = GithubActions.types.RunsOn.`ubuntu-18.04`
          , steps = setup
          }
        }
    }
