{ jdk ? "jdk17" }:

let
  config = {
    packageOverrides = p: rec {
      java = p.${jdk};

      sbt = p.sbt.overrideAttrs (
        old: rec {
          patchPhase = ''
            echo -java-home ${java} >> conf/sbtopts
          '';
        }
      );
    };
  };

  nixpkgs = fetchTarball {
    name   = "nixos-unstable-2021-01-03";
    url    = "https://github.com/NixOS/nixpkgs/archive/56bb1b0f7a3.tar.gz";
    sha256 = "1wl5yglgj3ajbf2j4dzgsxmgz7iqydfs514w73fs9a6x253wzjbs";
  };

  pkgs = import nixpkgs { inherit config; };
in
pkgs.mkShell {
  name = "scala-shell";

  buildInputs = [
    pkgs.coursier
    pkgs.gnupg
    pkgs.${jdk}
    pkgs.sbt
  ];
}
