let
  pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs-channels/archive/nixos-20.03.tar.gz") {};
  stdenv = pkgs.stdenv;

in stdenv.mkDerivation rec {
  name = "neutron";
  buildInputs = [
    pkgs.openjdk # v1.8.0_222
    pkgs.sbt  # v1.3.8
  ];
}
