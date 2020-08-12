# Replay Plugin

### Instructions
- prerequisite: local experiment database (copy)
- change database url, username and password in `src/main/resources/config.yml` 
- compile plugin with `./gradlew shadowJar`
- copy jar in the plugin folder of a new Minecraft server (without other plugins)
- start server
- open a text message with `t` and type `\select <gameid>`. 
- wait until you are teleported into a new world and watch the game
- messages from the system are white, message from the player are yellow
- WARNING: if you select a new game before the previous replay is finished, 
the plugin tries to jump between both which leads to a strobe light effect. 

### Intended behaviour
- select game from the database
- get game (placed and destroyed blocks, messages from player and system) played back
- pause and restart game
- jump to timestamp
- stop replay

### TODOs
- add explanation in start world
- stop old replay if a new game is started
- adjust replay time to original play time (currently to slow)
- implement pause/restart
- implement skipping
- stop being teleported into blocks
- maybe make replay smoother
