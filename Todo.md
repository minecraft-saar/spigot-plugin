# TODO
- bugfix: wool blocks turn into dirt blocks (has not happend since ground was changed to bedrock from grass)
- modern material vs. legacy material (this plugin uses modern material, the test plugin VirtualPlayers uses legacy material)
- make playerworlds smaller (50 * 50 or 20 * 20) --> worldborder, then don't load blocks outside of world border
- stop saving of playerworlds
- delete old player worlds. Both at start and end of server script
- bugfix: when a block is placed on a water block at ground level it merges with the water block and can't be destroyed.
- bugfix: on running server: when a player logs out and in again, the same world is present?


## Questions
- What should happen with their world when a player logs out? Their world can be deleted
- Are we interested in BlockDamageEvents?