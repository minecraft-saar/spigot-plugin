#! /bin/bash
# author: Arne Köhn <arne@chark.eu>
# License: Apache 2.0

set -e

SETUP_DIR=${1:-experiment-setup}
mkdir -p $SETUP_DIR
cd $SETUP_DIR

echo "press enter to kill broker, architect and minecraft server."
sleep 1

function setup {
# compile and set up the spigot server
mkdir spigot-server
cd spigot-server
wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
java -jar BuildTools.jar
java -jar spigot-1.14.4.jar
sed -i s/false/true/ eula.txt
cd ..

# compile and set up the plugin
git clone git@github.com:minecraft-saar/spigot-plugin.git
cd spigot-plugin/communication
./gradlew shadowJar
mkdir -p ../../spigot-server/plugins
cp build/libs/communication-1.0-SNAPSHOT-all.jar ../../spigot-server/plugins
cd ..
cp server_files/server.properties ../spigot-server
cd ..


git clone git@github.com:minecraft-saar/infrastructure.git
cd infrastructure
./gradlew build
cp broker/example-broker-config.yaml broker/broker-config.yaml
cd ..

touch .setup_complete
} # end setup function


if [[ ! -f .setup_complete ]]; then
	echo "running setup before starting the servers"
	setup
fi

echo "starting the dummy architect ..."
cd infrastructure
./gradlew architect:run &
sleep 2
cd ..


echo "starting the broker ..."
cd infrastructure
./gradlew broker:run &
sleep 5
cd ..

echo "starting minecraft server ..."
cd spigot-server
java -jar spigot-1.14.4.jar &

echo "press enter to kill broker, architect and minecraft server."
read
pkill -P $$
