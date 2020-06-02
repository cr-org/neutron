# neutron

[![CI Status](https://github.com/cr-org/neutron/workflows/Scala/badge.svg)](https://github.com/cr-org/neutron/actions)
[![MergifyStatus](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/cr-org/neutron&style=flat)](https://mergify.io)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

A *pulsar* is a celestial object, thought to be a rapidly rotating neutron star, that emits regular pulses of radio waves and other electromagnetic radiation at rates of up to one thousand pulses per second.

### How to use it

DISCLAIMER: It is currently a WIP so **there is no release and we do not recommend its wide usage yet**. However, if you would like to give it a try you can find the latest snapshots published here: [https://oss.sonatype.org/#nexus-search;quick~com.chatroulette](https://oss.sonatype.org/#nexus-search;quick~com.chatroulette).

Sooner rather than later we plan to make a first official release and write documentation so it can also be useful outside of our company. Stay tuned!

### Build

If you use [Nix Direnv](https://github.com/nix-community/nix-direnv) then you will be able to run the commands straight away. Otherwise, make sure you always enter a Nix Shell by running `nix-shell` at the project's root.

```
sbt +test
```

### Update CI build YAML

Using [Github Actions Dhall](https://github.com/regadas/github-actions-dhall).

```
cd .github/workflows
dhall-to-yaml --file scala.dhall > scala.yml
```
