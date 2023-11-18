# Handy Structures
It's a Minecraft mod made using Fabric API.

### Minecraft Versions:
- 1.14 Old (original) mod
- 1.20.2

### Commands Added:
To save structure mentioned in structure block at `pos`
(just like when that structure block is powered, but structure persisted on disk)
```
/structure save block <pos>
```

To save structure between `from` & `to` coordinates
```
/structure save <from> <to> <name> [<ignoreEntities>]
```

To remove structure mentioned in structure block at `pos`
```
/structure remove block <pos>
```

To remove structure by its name
```
/structure remove <name>
```


### Requires:
- [Fabric Loader](https://fabricmc.net/use/installer/)
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api/files)
- [Fabric Kotlin Lang](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin/files)