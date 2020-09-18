{ jdk ? "11" }:

let
  nixpkgs = fetchTarball {
    name   = "NixOS-unstable-13-05-2020";
    url    = "https://github.com/NixOS/nixpkgs-channels/archive/6bcb1dec8ea.tar.gz";
    sha256 = "04x750byjr397d3mfwkl09b2cz7z71fcykhvn8ypxrck8w7kdi1h";
  };

  matrix = p:
    if jdk == "14" then p.jdk14
    else if jdk == "11" then p.jdk11
    else p.jdk8;

  config = {
    packageOverrides = p: {
      sbt = p.sbt.override { jre = matrix p; };
    };
  };

  pkgs = import nixpkgs { inherit config; };
in
  pkgs.mkShell {
    buildInputs = [ (matrix pkgs) pkgs.sbt ];
  }
