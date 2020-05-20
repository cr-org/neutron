let Schemas =
      https://raw.githubusercontent.com/regadas/github-actions-dhall/master/schemas.dhall sha256:b42b062af139587666185c6fb72cc2994aa85a30065324174760b7d29a9d81c9

in Schemas.Step::{
    , id = None Text
    , name = None Text
    , uses = Some "olafurpg/setup-scala@v2"
    , run = None Text
    , with = None (List { mapKey : Text, mapValue : Text })
    }
