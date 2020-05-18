#! /bin/bash

cd server/plugins
pwd

WHITELIST_FILE=../../../../../whitelist.txt
echo $WHITELIST_FILE
if test -f "$WHITELIST_FILE"; then
    echo "exists"
    WHITELIST="$(<$WHITELIST_FILE)"
    while read line; do
        echo "Line No.: $line"
        PADDED="\ \ $line"
        sed -i "/NotBannedPlayers:/a $PADDED" CommunicationPlugin/config.yml
    done < $WHITELIST_FILE
fi

