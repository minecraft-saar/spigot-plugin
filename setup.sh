#! /bin/bash
# Author: Arne Köhn <arne@chark.eu>
# This script sets up a spigot server with
# the MC-Saar-Instruct plugin.
# This setup script is not idempotempt.
# run the server with cd server; java -jar spigot-1.14.4.jar
# (or whatever the version of the server is now)

set -e
set -u

mkdir -p server
cd server

if [[ -f .setup_complete ]]; then
    echo "setup already completed"
    exit
fi

wget https://papermc.io/api/v1/paper/1.15.2/350/download -O paper.jar
# this call will fail, but also sets up all the files
echo "please ignore the EULA warning below, this is part of the setup process"
java -jar paper.jar

# acknowledge EULA so we can start the server
sed -i s/false/true/ eula.txt

# build our plugin
(cd ../communication/ ; ./gradlew shadowJar)

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
rm -f server.properties
for f in server.properties bukkit.yml spigot.yml; do
    cp ../server_files/$f .
done

cd ..
touch .setup_complete
