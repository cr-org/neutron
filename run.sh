#! /usr/bin/env bash

docker-compose up -d

# wait for a container to be healthy
MAX_HEALTH_CHECK_TRIES=20

wait_for_pulsar(){
  for (( i=1 ; i <= MAX_HEALTH_CHECK_TRIES; i++ )); do

    RESULT=$(curl -f http://localhost:8080/admin/namespaces/public)
    if [[ ${RESULT} == *"public"* ]]; then
      echo -e "$1 is healthy!\n"
      break
    else
      echo "$1 not healthy.  Trying $i of ${MAX_HEALTH_CHECK_TRIES}. Sleeping 5 seconds."
      if [[ "$i" != "${MAX_HEALTH_CHECK_TRIES}" ]]; then
          sleep 5
      fi
    fi

    if [[ "$i" == "${MAX_HEALTH_CHECK_TRIES}" ]]; then
      echo -e "ERROR: $1 not healthy. Aborting"
      exit 1
    fi
  done
}

wait_for_pulsar

echo "Creating the public/neutron namespace"
docker-compose exec -T pulsar bin/pulsar-admin namespaces create public/neutron

echo "Setting BACKWARD schema compatibility strategy for the public/neutron namespace"
docker-compose exec -T pulsar bin/pulsar-admin namespaces set-schema-compatibility-strategy -c BACKWARD public/neutron

echo "Creating the public/nope namespace"
docker-compose exec -T pulsar bin/pulsar-admin namespaces create public/nope

echo "Setting ALWAYS_INCOMPATIBLE schema compatibility strategy for the public/nope namespace"
docker-compose exec -T pulsar bin/pulsar-admin namespaces set-schema-compatibility-strategy -c ALWAYS_INCOMPATIBLE public/nope

echo "Done"
