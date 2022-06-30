# Nether Ratio

A PaperMC plugin to allow customizable Nether to Overworld ratio.

## How to set up

Place the plugin jar into your **plugins** folder inside the server files.

**Keep in mind that portals created prior to this operation will not be recognized by the plugin.**

## Behavior

By default, this plugin will allow you to build portals out of **Crying Obsidian** and travel
at a ratio of **20:1**, instead of the vanilla **8:1** - meaning, every block you travel in the Nether dimension
will correspond to 20 blocks traveled in the Overworld.

Vanilla portals made out of **Obsidian** will keep their vanilla ratio of **8:1**.

A custom portal will only lead to another custom portal, and a normal portal to another normal portal.

The plugin will test whether other plugins allow the portal to be created or if the player may teleport
to it.

## Config

Several aspects of the plugin are configurable.

| Option                         | Valid values           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|--------------------------------|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ratio`                        | any positive number    | The defining ratio of the custom portals. <br/> Traveling `ratio.nether` blocks in the Nether dimension will correspond to traveling `ratio.overworld` in the Overworld. <br/> A ratio of `40:2` is equivalent to `20:1`. <br/><br/> _Defaults to `overworld: 20`, and `nether: 1`._                                                                                                                                                                                                                                                                                                                                                                                             |
| `frame_block`                  | any Minecraft block ID | The block the custom portal should be made out of. <br/> One may set this to any existent block in Minecraft, but expect weird behaviors when using weird blocks. <br/> Setting this to an invalid block ID will reset it to the default value. <br/> **Setting this to `"minecraft:obsidian"` will essentially replace the vanilla portals.** <br/><br/> _Defaults to `"minecraft:crying_obsidian"`._                                                                                                                                                                                                                                                                           |
| `allow_floating_placement`     | `true`/`false`         | Initially, the plugin attempts to create a portal on top of a buildable surface. If no such position is found and this value is set to `true`, the plugin will attempt to create a portal floating in the air. <br/><br/> _Defaults to `true`._                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `allow_forced_placement`       | `true`/`false`         | If the methods above still failed in creating a portal and this value is set to `true`, the plugin will force a portal creation by creating an air bubble to place it into. <br/> This is an extremely rare case, but it may result in player-placed blocks being overwritten if they are not protected by another plugin. <br/><br/> _Defaults to `true`._                                                                                                                                                                                                                                                                                                                      |
| `min_distance_between_portals` | any positive number    | **Two portals that are closer than `min_distance_between_portals` blocks to each other in a dimension may lead to the same portal in the other dimension.** <br/> Similarly, when looking for a portal to teleport to in the other dimension, if none is found within `min_distance_between_portals` blocks, a new one will be created. <br/><br/> _The recommended value for a dimension is the `ratio` of that dimension multiplied by `max_portal_placement_offset.horizontal` (see below)._ <br/><br/> _Defaults to `overworld: 320`, and `nether: 16`._                                                                                                                     |
| `max_portal_placement_offset`  | any positive number    | When a new portal needs to be created in the other dimension, a valid position is searched in a cylindrical shape, of range `max_portal_placement_offset.horizontal` and height `max_portal_placement_offset.vertical`. <br/> **This means that a high volume of blocks is checked, so increasing any of these values will decrease the performance.** <br/><br/> _Defaults to `vertical: 128`, and `horizontal: 16`._                                                                                                                                                                                                                                                           |                                                                                                                                         |
| `portal_size`                  | any positive number    | Sizes that define how small or how large portals may be, in `height` and `width`. <br/> **These measurements do NOT include the frame blocks.** <br/> The `min` sizes define the smallest possible portal. For example, in the absence of this plugin, Nether portals need a **width** of at least **2** and a **height** of at least **3**. <br/> The `max` sizes define the largest possible portal. <br/> The `new` sizes define the measurements of newly created portals, generated as a consequence of traveling to the other dimension. <br/><br/> _Defaults to `height.min: 1`, `height.max: 21`, `height.new: 3`, `width.min: 1`, `width.max: 21`, and `width.new: 2`._ |

## Commands

The plugin adds the custom command `/nrreload`, which is intended to be used after modifying the configuration file,
in order to avoid a server restart.

**To use this command, a player needs the `netherratio.nrreload` permission (or `netherratio.*`).**