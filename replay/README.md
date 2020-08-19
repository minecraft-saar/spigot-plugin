# Replay Plugin

### Instructions
- prerequisite: local experiment database (copy)
- change database url, username and password in `src/main/resources/config.yml` 
- change line `implementation files("/home/ca/Documents/Hiwi_Minecraft/spigot-plugin/communication/build/libs/communication-1.1-SNAPSHOT-all.jar")` 
in build.gradle to a local version of the communication plugin jar
- compile plugin with `./gradlew shadowJar`
- copy jar in the plugin folder of a new Minecraft server (without other plugins)
- start server
- open a text message with `t` and type `\select <gameid>`. 
- wait until you are teleported into a new world and watch the game
- text messages from the system are white, messages from the player are yellow, 
system logs are aqua. Logs of the types `BlocksCurrentObjectLeft`, 
`CurrentWorld`, `CurrentObject`, `NewOrientation`, `InitialPlan` and 
`GameId` are currently not printed.
- WARNING: if you select a new game before the previous replay is finished, 
the plugin tries to jump between both which leads to a strobe light effect. 

### Configuration
- url: database with Minecraft game logs
- user: username for the database
- password: password for the database
- updateFrequency: default 5, number of ticks between updates
- speed: default 1.0, normal speed (< 1 slower, > 1 faster)

### Intended behaviour
- select game from the database
- get game (placed and destroyed blocks, messages from player and system) played back
- ~~pause and restart game~~
- ~~jump to timestamp~~
- stop replay

### TODOs
- add explanation in start world
- stop being teleported into blocks
- maybe make replay smoother
