# RW-HPS - User Manual

RW-HPS User Manual  
This document is intended for users who are not familiar with development but wish to use RW-HPS. If you want to develop, please first read the [Development Documentation](../../zh/plugin/README.md).

## Starting RW-HPS Using the Pure Console Version

### Installation

* Direct Run
* Using Docker Container

### Understanding the Running Environment

The first run will initialize the running environment. The table below explains the purpose of each folder.

Configuration Explanation: [Server Configuration](Config.md)

|          Folder Name           | Purpose                               |
|:------------------------------:|:--------------------------------------|
|      `data/plugins`       | Stores plugins                          |
|        `data/maps`        | Stores maps                             |
|        `data/mods`        | Stores Rwmod                            |
|       `data/cache`        | Stores cache, generally not important   |
|        `data/libs`        | Stores dependencies, generally not important |
|      `data/CoreLib`       | Stores Hess core dependencies, generally not important |
|        `data/log`         | Stores logs, generally not important    |
|    `data/Config.json`     | Stores server configuration, can be opened and modified |
|  `data/ConfigServer.json` | Stores server configuration in Server mode |
| `data/RelayConfig.json`   | Stores server configuration in Relay mode |
|     `data/Test.json`      | Stores test options                     |
|    `data/Settings.bin`    | Stores internal configurations, generally not important |

### Download and Install Plugins

A newly installed RW-HPS does not have any custom functions. The functions will be provided by plugins.

### Using Mods

Just put the mods into `data/mods`!

### Using Maps

#### Custom Maps

Put the maps into `data/maps`, then refer to [How to Switch Maps](#how-to-switch-maps).

#### Using Saved Games

Change the file extension to `.save`, put it into `data/maps`, then refer to [How to Switch Maps](#how-to-switch-maps).

#### How to Switch Maps

Type `.maps` in the client chat box to view the maps in effect on the server.  
You will find a number ID in front of each name.  
Type `.map + space + ID` in the client chat box to switch to your custom map.  
**Note: After switching maps, the host's map display might have issues, this is normal and doesn't require any action.**  
**To switch back to the original version, just select one of the host's original maps.**

## Troubleshooting

If you encounter any issues or have suggestions, you can post and discuss them in  
[issues](https://github.com/deng-rui/RW-HPS/issues)  
[Tencent QQ Group](https://qm.qq.com/cgi-bin/qm/qr?k=qhJ6ekYF9pD9jO6j8H2rZw8ePAVypoU0&jump_from=webapi)  
[Telegram Group](https://t.me/RW_HPS) \
[DisCord Group](https://discord.gg/VwwxJhVG64)