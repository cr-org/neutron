# neutron

[![CI Status](https://github.com/cr-org/neutron/workflows/Scala/badge.svg)](https://github.com/cr-org/neutron/actions)
[![MergifyStatus](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/cr-org/neutron&style=flat)](https://mergify.io)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

A *pulsar* is a celestial object, thought to be a rapidly rotating neutron star, that emits regular pulses of radio waves and other electromagnetic radiation at rates of up to one thousand pulses per second.

[![pulsar](https://www.jpl.nasa.gov/spaceimages/images/largesize/PIA18845_hires.jpg "An accreting pulsar. Credit NASA/JPL-Caltech")](https://www.jpl.nasa.gov/spaceimages/?search=pulsar&category=#submit)

### Disclaimer   
**Chatroulette** uses this library in production as a base of the whole platform.      
However, this library is in active development so if you want to use it **do it at your own risk**. 

### Quick start
If you are using `SBT` just add it as one of your dependencies:
```
val neutronVersion = "0.0.1"

libraryDependencies += "com.chatroulette" %% "neutron-core" % neutronVersion     // Core module
libraryDependencies += "com.chatroulette" %% "neutron-function" % neutronVersion // Function module
```

That is it!

### Build
If you have `SBT` installed you don't have to worry about anything. Simply run `sbt +test` command in the project root to run the tests. 

If you are a `nix` user and you use [Nix Direnv](https://github.com/nix-community/nix-direnv) then you will be able to run the commands straight away. 
Otherwise, make sure you always enter a `Nix Shell` by running `nix-shell` at the project's root.

```
sbt +test
```

### Update CI build YAML
To update CI build you should have `dhall-json` installed or simply use `nix`.
Using [Github Actions Dhall](https://github.com/regadas/github-actions-dhall).

```
cd .github/workflows
dhall-to-yaml --file scala.dhall > scala.yml
```

