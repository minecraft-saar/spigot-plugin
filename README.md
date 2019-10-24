# spigot-plugin


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

## Files in this repository
### communication
Plugin for communication between Minecraft and broker and setting up Minecraft for experiments

### worldtest
Plugin for test purposes that does not communicate with broker

### server_files
Copies of files of the spigot server that I modified

## Running the Minecraft server
- start architect
- start broker
- start spigot server with plugin


## Links for Plugin Development
- [https://hub.spigotmc.org/javadocs/spigot/overview-summary.html](https://hub.spigotmc.org/javadocs/spigot/overview-summary.html)
- [https://www.spigotmc.org/wiki/spigot-plugin-development/](https://www.spigotmc.org/wiki/spigot-plugin-development/)


## Links for World Creation
- https://bukkit.gamepedia.com/Developing_a_World_Generator_Plugin
- https://bukkit.org/threads/how-to-create-custom-world-generators.79066/

## Related third-party plugins
- [https://dev.bukkit.org/projects/virtualplayers](https://dev.bukkit.org/projects/virtualplayers)