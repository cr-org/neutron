#! /usr/bin/env bash

echo "Creating the public/neutron namespace"
docker-compose exec pulsar bin/pulsar-admin namespaces create public/neutron

echo "Setting BACKWARD schema compatibility strategy for the public/neutron namespace"
docker-compose exec pulsar bin/pulsar-admin namespaces set-schema-compatibility-strategy -c BACKWARD public/neutron

echo "Creating the public/nope namespace"
docker-compose exec pulsar bin/pulsar-admin namespaces create public/nope

echo "Setting ALWAYS_INCOMPATIBLE schema compatibility strategy for the public/nope namespace"
docker-compose exec pulsar bin/pulsar-admin namespaces set-schema-compatibility-strategy -c ALWAYS_INCOMPATIBLE public/nope

echo "Done"
