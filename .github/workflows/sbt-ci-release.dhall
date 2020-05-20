let Schemas =
      https://raw.githubusercontent.com/regadas/github-actions-dhall/master/schemas.dhall sha256:b42b062af139587666185c6fb72cc2994aa85a30065324174760b7d29a9d81c9

in    λ(args : { ref : Text, pgpPassphrase: Text, pgpSecret: Text, sonatypePassword: Text, sonatypeUsername: Text })
    → Schemas.Step::{
      , id = None Text
      , name = Some "Publish ${args.ref}"
      , uses = None Text
      , run = Some "sbt ci-release"
      , env = Some
          ( toMap
              { PGP_PASSPHRASE = args.pgpPassphrase
              , PGP_SECRET = args.pgpSecret
              , SONATYPE_PASSWORD = args.sonatypePassword
              , SONATYPE_USERNAME = args.sonatypeUsername
              }
          )
      }
