#! /bin/bash
# author: Arne Köhn <arne@chark.eu>
# License: Apache 2.0

SPIGOT_VERSION=${1:-1.15.2}

cd $(dirname $0)

if [[ ! -f .setup_complete ]]; then
    ./setup.sh $SPIGOT_VERSION
fi

cd server
java -jar spigot-$SPIGOT_VERSION.jar 
