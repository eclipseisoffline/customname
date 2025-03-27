- Update to 1.21.5.
- Player prefixes, nicknames, and suffixes are now stored as NBT text components, rather than JSON strings.

Please note! With this update, changes have been made to the way player names are stored. Backwards compatibility has
been provided, and names from 1.21.4 and older should load fine, however it is recommended to take backups. Please report
any bugs on the issue tracker!

With this change in how player names are stored, backwards compatibility with version `0.1.0-1.20.4` is no longer supported.
Custom names in worlds that last ran that version will break. To fix this, load the world first with any version of the
mod before `0.3.0-1.21.5`, and then load the world in this version.
