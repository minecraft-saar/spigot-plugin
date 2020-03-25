# spigot-plugin

## fast default setup

run ./start.sh to download and compile and everything except the Minecraft client.

## Setting up a spigot server
1. Download `BuildTools.jar` and run it according to the instructions in [https://www.spigotmc.org/wiki/buildtools/](https://www.spigotmc.org/wiki/buildtools/)
2. Copy the generated server jar into a new directory (the directory where you want the server to be)
3. Create a start up script for your operating system as described in [https://www.spigotmc.org/wiki/spigot-installation/#installation](https://www.spigotmc.org/wiki/spigot-installation/#installation)
4. Replace the generated file `server.properties` with the version from `server_files` in this repository
5. Generate the communication plugin and copy it into `<your_server_directory>/plugins/`

## Creating plugin
`> cd communication/`  
`> ./gradlew shadowJar`

then copy generated jar from `.../spigot-plugin/communication/build/libs/communication-1.0-SNAPSHOT-all.jar` to the plugin folder of the spigot server

## Setting up a Wizard of Oz server
1. Set up a second Minecraft server running on a different port than the game Minecraft server
2. Create the Wizard of Oz plugin with `./gradlew shadowJar` and copy it to the plugin folder of the server
3. Start the WOZ-Minecraft server
4. Start a broker
5. Log in to WOZ-Minecraft server (as the wizard)
6. Start the game Minecraft server
7. A player logging in to the game Minecraft server now gets their instruction from the wizard

## Running the Minecraft server
- start architect
- start broker
- start spigot server with plugin


## Files in this repository
### communication
- CommunicationPlugin: Plugin for communication between Minecraft and broker and setting up Minecraft for experiments, is created with `> ./gradlew shadowJar`
- WorldTestPlugin: Plugin for test purposes that does not communicate with broker, is created with `> ./gradlew worldTestShadowJar`
- DefaultPlugin: contains shared methods of both plugins
- Client: Interface for the clients
- MinecraftClient: Handles communication with the broker
- DummyMinecraftClient: For testing, hard-codes broker responses
- MinecraftListener: Reacts to changes on the MinecraftServer 
- FlatChunkGenerator: Helper class for creating flat, empty worlds


### woz
Plugin for Wizard of Oz experiments, substitutes an architect server and architect.

Only one player can log in to its server to give instructions.

## worldbuilder
Plugin to create initial world states which can later be loaded by the communication plugin
- start a Minecraft Server with the compiled `WorldBuilderPlugin`
- log in a player
- build the structure that should be the initial world state
- open the chat and enter `\save <filename.csv>`

### server_files
Copies of files of the spigot server that I modified




## Links for Plugin Development
- [https://hub.spigotmc.org/javadocs/spigot/overview-summary.html](https://hub.spigotmc.org/javadocs/spigot/overview-summary.html)
- [https://www.spigotmc.org/wiki/spigot-plugin-development/](https://www.spigotmc.org/wiki/spigot-plugin-development/)


## Links for World Creation
- https://bukkit.gamepedia.com/Developing_a_World_Generator_Plugin
- https://bukkit.org/threads/how-to-create-custom-world-generators.79066/

## Related third-party plugins
- [https://dev.bukkit.org/projects/virtualplayers](https://dev.bukkit.org/projects/virtualplayers): For executing player commands without a real player
- [https://www.spigotmc.org/resources/timed-scripts.28121/](https://www.spigotmc.org/resources/timed-scripts.28121/): For running automated test games
