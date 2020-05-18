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

if [[ -f .setup_complete ]]; then
    echo "setup already completed"
    exit
fi

wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
java -jar BuildTools.jar --rev $SPIGOT_VERSION
java -jar spigot-$SPIGOT_VERSION.jar
sed -i s/false/true/ eula.txt

(cd ../communication/ ; ./gradlew shadowJar)

mkdir -p plugins
cd plugins
ln -s ../../communication/build/libs/communication-*-all.jar .

# copy the local whitelist
mkdir CommunicationPlugin
cp ../../communication/src/main/resources/config.yml CommunicationPlugin/
WHITELIST_FILE=../../../../../whitelist.txt
if test -f "$WHITELIST_FILE"; then
    WHITELIST="$(<$WHITELIST_FILE)"
    while read line; do
        sed -i "/NotBannedPlayers:/a \ \ $line" CommunicationPlugin/config.yml
    done < $WHITELIST_FILE
fi

cd ..
rm -f server.properties
for f in server.properties bukkit.yml spigot.yml; do
    ln -s ../server_files/$f .
done

cd ..
touch .setup_complete
