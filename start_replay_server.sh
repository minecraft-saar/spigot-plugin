#! /bin/bash
# author: Arne Köhn <arne@chark.eu>
# License: Apache 2.0

set -e
set -u

cd $(dirname $0)

if [[ ! -f .setup_complete ]]; then
    ./setup.sh
fi

cd replay_server
java -jar paper.jar 
