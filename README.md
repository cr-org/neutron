# neutron

[![CI Status](https://github.com/cr-org/neutron/workflows/Scala/badge.svg)](https://github.com/cr-org/neutron/actions)

A *pulsar* is a celestial object, thought to be a rapidly rotating neutron star, that emits regular pulses of radio waves and other electromagnetic radiation at rates of up to one thousand pulses per second.

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
