[download]: https://img.shields.io/github/downloads/Mvndi/TemplateWorldRegenerator/total
[downloadLink]: https://github.com/Mvndi/TemplateWorldRegenerator/releases
[discord-shield]: https://img.shields.io/discord/728592434577014825?label=discord
[discord-invite]: https://discord.gg/RPNbtRSFqG

[ ![download][] ][downloadLink]
[ ![discord-shield][] ][discord-invite]

[**Discord**](https://discord.gg/RPNbtRSFqG) | [**Hangar**](https://hangar.papermc.io/Hydrolien/TemplateWorldRegenerator) | [**Modrinth**](https://modrinth.com/plugin/templateworldregenerator) | [**GitHub**](https://github.com/Mvndi/TemplateWorldRegenerator)

# TemplateWorldRegenerator
A Paper/Folia plugin that regenerates selected chunks from a template world into a live world.

## Usage

Download the latest version from [the releases](https://modrinth.com/plugin/templateworldregenerator). Start your server. Run command to replace an area while avoiding player builds.

## Statistics
[![bStats Graph Data](https://bstats.org/signatures/bukkit/templateworldregenerator.svg)](https://bstats.org/plugin/bukkit/TemplateWorldRegenerator/31503)

## Build
`./gradlew assemble`
The plugin jar file will be in build/libs/


## TODO

- Kill old mobs and replace them with the template world ones.
- Try with mvndi datapack in both world.
- Copy biomes from the template world
- Allow large area to be copy paste with progess display as in chunky
- Avoid Town + x chunks when copypasting large area (Towny)
- Avoid Roads when copypasting large area (TownyRoads)
- Avoid deep ocean when when copypasting large area (from configured biome)



### TODO minor world improvement

- Custom copy rule for leaves in chunks that are at the border:
  - Add the leaves that are outside the regenerated area but that replace air and are connected to logs inside the regenerated area.
  - Remove the leaves that have been placed but that are not connected to logs



## Note

```
/world import "world_template" worlds:world_template dimension minecraft:overworld
/tp 105852 100 30588
/world teleport minecraft:overworld Hydrolien ~ ~ ~
/world teleport mvndi:overworld Hydrolien ~ ~ ~
/world teleport worlds:world_template Hydrolien ~ ~ ~
```