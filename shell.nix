let
  nixpkgs = fetchTarball {
    name   = "NixOS-unstable-13-05-2020";
    url    = "https://github.com/NixOS/nixpkgs-channels/archive/6bcb1dec8ea.tar.gz";
    sha256 = "04x750byjr397d3mfwkl09b2cz7z71fcykhvn8ypxrck8w7kdi1h";
  };
  config = {
    packageOverrides = p: {
      sbt = p.sbt.override { jre = p.jdk11; };
    };
  };
  pkgs = import nixpkgs { inherit config; };
in
  pkgs.mkShell {
    buildInputs = [ pkgs.jdk11 pkgs.sbt ];
  }
