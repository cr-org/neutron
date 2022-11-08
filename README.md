# neutron

[![CI Status](https://github.com/cr-org/neutron/workflows/Scala/badge.svg)](https://github.com/cr-org/neutron/actions)
[![MergifyStatus](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/cr-org/neutron&style=flat)](https://mergify.io)
[![Maven Central](https://img.shields.io/maven-central/v/com.chatroulette/neutron-core_2.13.svg)](https://search.maven.org/search?q=com.chatroulette.neutron)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

A *pulsar* is a celestial object, thought to be a rapidly rotating neutron star, that emits regular pulses of radio waves and other electromagnetic radiation at rates of up to one thousand pulses per second.

[![pulsar](https://www.jpl.nasa.gov/spaceimages/images/largesize/PIA18845_hires.jpg "An accreting pulsar. Credit NASA/JPL-Caltech")](https://www.jpl.nasa.gov/spaceimages/?search=pulsar&category=#submit)

### Disclaimer

**Chatroulette** uses this library in production as the base of the whole platform. However, this library is in active development so if you want to use it, **do it at your own risk**.

### Documentation

Check out the [microsite](https://cr-org.github.io/neutron/).

### Pulsar version

At the moment, we target Apache Pulsar 2.10.x.

### Development

If you have `sbt` installed, you don't have to worry about anything. Simply run `sbt +test` command in the project root to run the tests.

If you are a `nix` user, make sure you enter a `Nix Shell` by running `nix-shell` at the project's root.

```
sbt +test
```

Remember to first start Pulsar and initialize it with docker-compose

```
./docker-compose up -d
```

### Schemas

Working with schemas when using our Pulsar `docker-compose` configuration.

Get [schema compatibility strategy](https://pulsar.apache.org/docs/en/schema-evolution-compatibility/#schema-compatibility-check-strategy):

```
$ docker-compose exec pulsar bin/pulsar-admin namespaces get-schema-compatibility-strategy public/default
FULL
```

Set schema compatibility strategy:

```
$ docker-compose exec pulsar bin/pulsar-admin namespaces set-schema-compatibility-strategy -c BACKWARD public/default
```
