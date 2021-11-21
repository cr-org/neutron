{ java ? "jdk17" }:

let
  pkgs = import ./pkgs.nix { inherit java; };
in
  pkgs.mkShell {
    buildInputs = [ pkgs.sbt ];
  }
