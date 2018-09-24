#!/bin/bash

# Networking
docker network create dvs
NETWORK_INTERFACE_NAME=$(route | grep '^default' | grep -o '[^ ]*$')
NETWORK_INTERFACE_ADDRESS=$(ip addr show $NETWORK_INTERFACE_NAME | grep -Po 'inet \K[\d.]+')

# Start Consul
docker run -d -p 8500:8500 --name=registry --network=dvs consul agent --dev -ui

# Start Databases
docker run -d --publish=7474:7474 --publish=7687:7687 --env=NEO4J_AUTH=none --network=dvs --name=graphdb neo4j
docker run -p 27017:27017 --name docdb -d --network=dvs mongo

# CrazyIvan and CLyman
docker run --name crazyivan --network=dvs -p 8766:8766 -p 8764:8764/udp -d aostreetart/crazyivan:v2 consul=http://registry:8500 ivan.prod.neo4j=neo4j://graphdb:7687 ivan.prod.http.host=crazyivan ivan.prod.http.port=8766 ivan.prod.udp.port=8764
docker run --name clyman --network=dvs -p 8768:8768 -p 8762:8762/udp -d aostreetart/clyman:v2 consul=http://registry:8500 clyman.prod.mongo=mongodb://docdb:27017 clyman.prod.http.host=clyman clyman.prod.http.port=8768 clyman.prod.udp.port=8762 clyman.prod.event.destination.host=$NETWORK_INTERFACE_ADDRESS clyman.prod.event.destination.port=8764
