#!/bin/sh

set -x -e -o pipefail -u

. .env

export JVM_OPTS="-Djavax.net.ssl.keyStore=$BETFAIR_CERT_FILE -Djavax.net.ssl.keyStorePassword=$BETFAIR_CERT_PASSWORD"

sbt "example-websocket/run"
