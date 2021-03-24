{ java }:

let
  nixpkgs = builtins.fetchTarball {
    name   = "nixos-unstable-2021-01-03";
    url    = "https://github.com/NixOS/nixpkgs/archive/56bb1b0f7a3.tar.gz";
    sha256 = "1wl5yglgj3ajbf2j4dzgsxmgz7iqydfs514w73fs9a6x253wzjbs";
  };

  config = {
    packageOverrides = p: {
      sbt = p.sbt.override {
        jre = p.${java};
      };
    };
  };

  pkgs = import nixpkgs { inherit config; };
in
  pkgs
