let
  # unstable packages on May 13th
  pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs-channels/archive/6bcb1dec8ea.tar.gz") {};
  stdenv = pkgs.stdenv;

in stdenv.mkDerivation rec {
  name = "neutron";
  buildInputs = [
    pkgs.haskellPackages.dhall-json
    pkgs.openjdk # 1.8.0_242
    pkgs.sbt # 1.3.10
  ];
}
