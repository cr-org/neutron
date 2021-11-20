{ java ? "jdk17" }:

let
  pkgs = import nix/pkgs.nix { inherit java; };
in
  pkgs.mkShell {
    buildInputs = [
      pkgs.gnupg
      pkgs.${java}
      pkgs.sbt
    ];
  }
