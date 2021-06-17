#! /bin/bash
# Author: Arne Köhn <arne@chark.eu>
# This script sets up a paper Minecraft server with
# the MC-Saar-Instruct plugin.
# This setup script is not idempotempt.
# run the server with start.sh
# run the replay server with start_replay_server.sh

set -e
set -u

USE_DEV_SERVER=${USE_DEV_SERVER:-false}

function setup_server {
    if $USE_DEV_SERVER; then
	../build_dev_paper.sh
	cp Paper-Server/build/libs/Paper*-mojang-mapped.jar paper.jar
    else
	wget https://papermc.io/api/v2/projects/paper/versions/1.16.5/builds/700/downloads/paper-1.16.5-700.jar -O paper.jar
    fi
    # this call will fail, but also sets up all the files
    echo "please ignore the Error and EULA warning below, this is part of the setup process"
    java -jar paper.jar

    # acknowledge EULA so we can start the server
    sed -i s/false/true/ eula.txt
    rm -f server.properties
    for f in server.properties bukkit.yml spigot.yml; do
        cp ../server_files/$f .
    done
}

#### Build shared parts of Minecraft server

if [[ -f .setup_complete ]]; then
    echo "setup already completed"
    echo "remove the .setup_complete file to force a new setup"
    exit
fi

rm -rf server replay_server

mkdir -p server
cd server
setup_server
cd ..

# duplicate setup
cp -R server replay_server

# build our plugins
(
    cd communication/
    ./gradlew shadowJar
    ./gradlew publishToMavenLocal
    cd ../replay
    ./gradlew shadowJar
)

# copy plugin and configuration files to server directory
(
    cd server
    # deploy plugin
    mkdir -p plugins
    cd plugins
    ln -s ../../communication/build/libs/communication-*-all.jar .
    # copy the local whitelist
    mkdir CommunicationPlugin
    cp ../../communication/src/main/resources/config.yml CommunicationPlugin/
    WHITELIST_FILE=~/minecraft-software/whitelist.txt
    if [[ -f $WHITELIST_FILE ]]; then
        while read line; do
            sed -i "/NotBannedPlayers:/a \ \ $line" CommunicationPlugin/config.yml
        done < $WHITELIST_FILE
    fi
    cd ..
    cd ..
)

(
    cd replay_server
    # make the server run on a different port
    sed -i 's/server-port=25565/server-port=25567/' server.properties
    mkdir -p plugins
    cd plugins
    ln -s ../../replay/build/libs/replay-*-all.jar .
)

touch .setup_complete
