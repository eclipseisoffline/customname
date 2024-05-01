# Custom Names

This mod adds a `/name` and a `/itemname` command to Minecraft, which players can use to set a prefix, suffix, or nickname,
or give their items colourful names. Mostly designed to be used in small, private servers.

Supports prefixes/suffixes set with LuckPerms as well! 

Feel free to report any bugs, or suggest new features, at the issue tracker.

## License

This mod is licensed under GNU GPLv3.

## Donating

If you like this mod, consider [donating](https://ko-fi.com/eclipseisoffline)!
It really helps me a ton!

## Usage

Mod builds can be found [here](https://github.com/eclipseisoffline/customname/packages/2065010) and on [Modrinth](https://modrinth.com/mod/fabric-custom-names).

This mod is oriented at Fabric Minecraft servers, but works on the client as well.
This mod requires the Fabric API, and is currently available for Minecraft 1.20.5+6, 1.20.1 and 1.19.4 (1.20.4 and 1.19.2 ports exist, but are no longer updated).
You can make a version port request at the issue tracker.

The `/name` command can be used as follows:

- `/name prefix` - sets a prefix for your name, or when no prefix is given, clears your prefix.
  - Requires operator or the `customname.prefix` permission.
- `/name suffix` - sets a suffix for your name, or when no suffix is given, clears your suffix.
  - Requires operator or the `customname.suffix` permission.
- `/name nickname` - sets a nickname that will appear instead of your IGN. When hovered above this nickname, your IGN will show. When no nickname is given, clears your nickname.
  - Requires operator or the `customname.nick` permission.

The `/itemname` command can be used to rename the item you're currently holding. Requires operator or the `customname.itemname` permission.

Minecraft's [formatting codes](https://minecraft.wiki/w/Formatting_codes) can be used to format your prefix, suffix or nickname.
Instead of the `§` character, use `&`. Alongside Minecraft's default formatting codes, the `&#<hex code>` format can be used as well.

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
  "blacklisted_names": []
}
```

- `enable_formatting` can be used to disable the use of Minecraft formatting codes in names.
- `require_permissions` can be used to disable the permission requirement. When set to `false`, the `/name` and `/itemname` commands are available to everyone.
- `blacklisted_names` is a list of regexes that are blacklisted. When a prefix, suffix or nickname matches one of these regexes, they won't be able to be used.
