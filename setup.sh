#! /bin/bash
# Author: Arne Köhn <arne@chark.eu>
# This script sets up a spigot server with
# the MC-Saar-Instruct plugin.
# This setup script is not idempotempt.
# run the server with cd server; java -jar spigot-1.14.4.jar
# (or whatever the version of the server is now)

SPIGOT_VERSION=1.14.4

set -e
set -u

mkdir -p server
cd server

wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
java -jar BuildTools.jar --rev $SPIGOT_VERSION
java -jar spigot-$SPIGOT_VERSION.jar
sed -i s/false/true/ eula.txt

(cd ../communication/ ; ./gradlew shadowJar)

mkdir -p plugins
cp ../communication/build/libs/communication-*-all.jar plugins
cp ../server_files/server.properties .
