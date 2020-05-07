#! /bin/bash
# Author: Arne Köhn <arne@chark.eu>
# This script sets up a spigot server with
# the MC-Saar-Instruct plugin.
# This setup script is not idempotempt.
# run the server with cd server; java -jar spigot-1.14.4.jar
# (or whatever the version of the server is now)

set -e
set -u

SPIGOT_VERSION=${1:-1.15.2}

mkdir -p server
cd server

wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
java -jar BuildTools.jar --rev $SPIGOT_VERSION
java -jar spigot-$SPIGOT_VERSION.jar
sed -i s/false/true/ eula.txt

(cd ../communication/ ; ./gradlew shadowJar)

mkdir -p plugins
cd plugins
ln -s ../../communication/build/libs/communication-*-all.jar .
cd ..
rm -f server.properties
ln -s ../server_files/server.properties .

cd ..
touch .setup_complete
