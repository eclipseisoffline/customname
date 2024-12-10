# Custom Names

[![Modrinth Version](https://img.shields.io/modrinth/v/saIlazMs?logo=modrinth&color=008800)](https://modrinth.com/mod/fabric-custom-names)
[![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/saIlazMs?logo=modrinth&color=008800)](https://modrinth.com/mod/fabric-custom-names)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/saIlazMs?logo=modrinth&color=008800)](https://modrinth.com/mod/fabric-custom-names)
[![Discord Badge](https://img.shields.io/badge/chat-discord-%235865f2)](https://discord.gg/CNNkyWRkqm)
[![Github Badge](https://img.shields.io/badge/github-customname-white?logo=github)](https://github.com/eclipseisoffline/customname)
![GitHub License](https://img.shields.io/github/license/eclipseisoffline/customname)

This mod adds a `/name`, a `/itemname` and a `/itemlore` command to Minecraft, which players can use to set a prefix, suffix, or nickname,
or give their items colourful names and lore. Mostly designed to be used in small, private servers.

Supports prefixes/suffixes set with LuckPerms as well! 

Feel free to report any bugs, or suggest new features, at the issue tracker.

## License

This mod is licensed under GNU LGPLv3.

## Donating

If you like this mod, consider [donating](https://buymeacoffee.com/eclipseisoffline).

## Discord

For support and/or any questions you may have, feel free to join [my discord](https://discord.gg/CNNkyWRkqm).

## Version support

| Minecraft Version | Actively developed       |
|-------------------|--------------------------|
| 1.21.4            | ✅ Current                |
| 1.21.2+3          | ❌ Available, unsupported |
| 1.21+1            | ✅ Current                |
| 1.20.5+6          | ❌ Available, unsupported |
| 1.20.4            | ❌ Available, unsupported |
| 1.20.1            | ❌ Available, unsupported |
| 1.19.4            | ❌ Available, unsupported |
| 1.19.2            | ❌ Available, unsupported |

I try to keep support up for the latest major and latest minor release of Minecraft. Updates to newer Minecraft
versions may be delayed from time to time, as I do not always have the time to immediately update my mods.

## Usage

Mod builds can be found [here](https://github.com/eclipseisoffline/customname/packages/2065010) and on [Modrinth](https://modrinth.com/mod/fabric-custom-names).

This mod is oriented at Fabric Minecraft servers, but works on the client as well. This mod requires the Fabric API.

The `/name` command can be used as follows:

- `/name prefix` - sets a prefix for your name, or when no prefix is given, clears your prefix.
  - Requires operator or the `customname.prefix` permission.
- `/name suffix` - sets a suffix for your name, or when no suffix is given, clears your suffix.
  - Requires operator or the `customname.suffix` permission.
- `/name nickname` - sets a nickname that will appear instead of your IGN, or when no nickname is given, clears your nickname.
  - Requires operator or the `customname.nick` permission.
- `/name other <prefix|suffix|nickname> <player>`
  - Same syntax as their respective `/name <prefix|suffix|nickname>` commands, but to set another player's prefix/suffix/nickname. Requires operator or the `customname.other` permission on top of the respective `customname.<nametype>` permission.

When hovering over a player's name with advanced tooltips enabled, their real name will show up.

The `/itemname` command can be used to rename the item you're currently holding. Requires operator or the `customname.itemname` permission.

Similarly, the `/itemlore` can be used to set the lore of an item you're holding. Requires operator or the `customname.itemlore` permission.
`\n` can be used to create new lines, `\\` can be used to escape a backslash.

Similar to the `/name` commands, running these commands without arguments resets the item name/lore of the item you're holding.

Minecraft's [formatting codes](https://minecraft.wiki/w/Formatting_codes) can be used to format your prefix, suffix, nickname, or your item names/lores.
Instead of the `§` character, use `&` (to use `&` in an (item)name, type `&&`).
Alongside Minecraft's default formatting codes, the `&#<hex code>` format can be used as well.

Useful tools to easily create formatted names are available [here](http://mcnick.surge.sh/) and [here](https://nickgen.netlify.app/).
Usage of formatting codes in names can be disabled in the config file.

Alongside the `/name` command, this mod also supports reading prefixes and suffixes from the LuckPerms mod.

## Config file

The mod's configuration file is present in `{root config directory}/eclipsescustomname.json`.
By default, the configuration file looks like this:

```json
{
  "enable_formatting": true,
  "require_permissions": true,
  "blacklisted_names": [],
  "max_name_length": 16,
  "operators_bypass_restrictions": false
}
```

- `enable_formatting` can be used to disable the use of Minecraft formatting codes in names.
- `require_permissions` can be used to disable the permission requirement. When set to `false`, the `/name`, `/itemname` and `/itemlore` commands are available to everyone.
- `blacklisted_names` is a list of regexes that are blacklisted. When a prefix, suffix or nickname matches one of these regexes, they won't be able to be used.
- `max_name_length` controls how long a player prefix/nickname/suffix can be, which can be 32 at most.
- `operators_bypass_restrictions` can be used to disable name restrictions for operators. When this is enabled, operators and people with the permission `customname.bypass_restrictions` can use spaces in nicknames, bypass the max length restriction, and more.
