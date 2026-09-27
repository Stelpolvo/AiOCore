# AiOCore

> A personal, all-in-one Bukkit plugin for **Minecraft 1.18 through the current release**.

AiOCore is a lightweight, modular core for survival and small-to-medium servers.
Instead of installing a pile of separate plugins, it provides a multi-currency
economy, a fully styleable chat system and pluggable player-data storage behind a
single `/aio` command and a small developer API.

---

## 🧭 Compatibility

| | |
| --- | --- |
| **Minecraft** | **1.18 – 26.x** (Spigot, Paper and forks) |
| **Java** | **17 or newer** (required) |
| **`api-version`** | `1.18` |

### Upward compatibility

AiOCore uses a conservative Bukkit API surface and is intended to keep working
on Spigot/Paper 1.18 and the current release line.

## ✨ Features

### 💰 Multi-currency economy
- Unlimited currencies defined in `config.yml` (`金币`, `点券`, …)
- Full **Vault** integration: the currency named in `economy.vault` is registered
  as a Vault `Economy` service, so any Vault-aware plugin can use it
- Player-to-player transfers with a per-currency `transferable` switch
- Admin commands: `get` / `set` / `give` / `take`
- Per-currency singular/plural names, fractional digits, format and symbol

### 💬 Chat system
- **Channels** — unlimited channels, each with its own name and permission.
  Messages are only rendered for players sitting in the sender's channel; the
  per-player choice is stored in the player's data file and restored on join
- **Four independent style categories** that combine freely
  - *name* — gradient colours for the player name
  - *message* — gradient colours for the message body
  - *chat* — layout template using `%player%`, `%message%`, `%server%`
  - *sound* — per-message sound feedback, written as `key;volume;pitch`
- **Cross-server chat** over the `BungeeCord` plugin channel
  (`PLUGIN_MESSAGE` forwarding, no proxy-side plugin needed) with echo
  filtering through `server-id`
- **Style preview** — `/aio chat <type> show` prints every style of a category
  with the permission it needs

### 🗄️ Flexible storage
- **YAML** — one `<uuid>.yml` per player, only changed sections are written
- **SQLite** — single-file database, zero configuration
- **MySQL** — pooled through HikariCP for networks and multi-server setups
- All three share one interface; switching is a one-line config change

### 🔌 PlaceholderAPI
`%aio_economy_<currency>%` and its `_formatted`, `_display`, `_symbol`, `_name`
variants, plus `%aio_economy_<currency>_player_<name>%` for other players.

### 🧑‍💻 Developer API
An `AiO` service is registered in Bukkit's `ServicesManager`, exposing the
economy, chat, player-data and message managers to other plugins.

## 📋 Commands

Root command: `/aio` (alias `/a`).

> The economy sub-command is registered as **`money`**.

### Economy

| Command | Permission | Description |
| --- | --- | --- |
| `/aio money pay <player> <currency> <amount>` | `aio.command.def.economy.pay` | Transfer currency to another player (players only) |
| `/aio money look <currency>` | `aio.command.def.economy.look` | Show your own balance (players only) |
| `/aio money get <player> <currency>` | `aio.command.admin.economy.get` | Query another player's balance |
| `/aio money set <player> <currency> <amount>` | `aio.command.admin.economy.set` | Set a player's balance |
| `/aio money give <player> <currency> <amount>` | `aio.command.admin.economy.give` | Add currency to a player |
| `/aio money take <player> <currency> <amount>` | `aio.command.admin.economy.take` | Remove currency from a player |

### Chat

| Command | Permission | Description |
| --- | --- | --- |
| `/aio chat <type> show` | `aio.command.def.chat.<type>` | Preview every style of a category (players only) |
| `/aio chat <type> set <key>` | `aio.command.def.chat.<type>` | Apply a style to yourself |
| `/aio chat <type> set <key> <player>` | `aio.command.def.chat.<type>` | Apply a style to another player |

`<type>` is one of `name`, `message`, `chat`, `sound`, `channel`.

Applying a style requires the *target player* to hold the permission attached to
that style or channel in `config.yml` (for example `aio.chat.style.name.gold`).

## 🔐 Permissions

| Permission | Default | Grants |
| --- | --- | --- |
| `aio.command.def` | everyone | `aio.command.def.economy` + `aio.command.def.chat` |
| `aio.command.def.economy` | everyone | `…economy.pay` + `…economy.look` |
| `aio.command.def.economy.pay` | everyone | `/aio money pay` |
| `aio.command.def.economy.look` | everyone | `/aio money look` |
| `aio.command.def.chat` | everyone | the five `…chat.<type>` nodes below |
| `aio.command.def.chat.name` | everyone | `/aio chat name …` |
| `aio.command.def.chat.message` | everyone | `/aio chat message …` |
| `aio.command.def.chat.chat` | everyone | `/aio chat chat …` |
| `aio.command.def.chat.sound` | everyone | `/aio chat sound …` |
| `aio.command.def.chat.channel` | everyone | `/aio chat channel …` |
| `aio.command.admin` | OP | `aio.command.def` + `aio.command.admin.economy` |
| `aio.command.admin.economy` | OP | `…economy.get` / `.set` / `.take` / `.give` |
| `aio.command.admin.economy.get` | OP | `/aio money get` |
| `aio.command.admin.economy.set` | OP | `/aio money set` |
| `aio.command.admin.economy.take` | OP | `/aio money take` |
| `aio.command.admin.economy.give` | OP | `/aio money give` |

Style and channel permissions are **not** declared in `plugin.yml` — they live in
`config.yml` (for example `aio.chat.style.name.gold`,
`aio.chat.channel.global`). Bukkit treats an undeclared permission as `false`
for non-operators, so grant them with a permissions plugin (LuckPerms, …)
otherwise only OPs can pick a style.

The chat command permissions above only control what appears in tab completion;
whether a style can actually be applied is decided by that style's own
permission from `config.yml`.

## 🧩 Placeholders

Identifier: `aio` (PlaceholderAPI must be installed).

| Placeholder | Result |
| --- | --- |
| `%aio_economy_<currency>%` | Raw balance (`100.0`) |
| `%aio_economy_<currency>_formatted%` | Balance run through the currency's `format` |
| `%aio_economy_<currency>_display%` | `format` with `%amount%` and `%symbol%` substituted |
| `%aio_economy_<currency>_symbol%` | The currency symbol |
| `%aio_economy_<currency>_name%` | The plural currency name |
| `%aio_economy_<currency>_player_<name>%` | Another player's balance (all suffixes above work) |

## ⚙️ Configuration

Everything lives in `plugins/AiOCore/config.yml`. A trimmed example:

```yaml
settings:
  save-interval-seconds: 10        # flush interval for the in-memory cache (min. 1)
  storage:
    type: "yml"                    # yml | sqlite | mysql
    url: "/data"                   # see the storage table below
    pool-size: 10                  # sqlite / mysql only
    username: "admin"
    password: "admin"
  messages:
    prefix: "&8[&cAiOCore&8] &r"
    no-permission: "&cYou do not have the authority to perform this operation &8(&7{permission}&8)"

economy:
  enabled: true
  vault: "coin"                     # which currency Vault sees
  currencies:
    coin:
      name-singular: "coin"
      name-plural: "coin"
      fractional-digits: 2
      format: "%amount%"
      symbol: "¤"
      transferable: true

chat:
  enabled: true
  channels:
    default:
      permission: "aio.chat.channel.def"
      name: "默认频道"
  cross-server:
    enabled: false
    type: "PLUGIN_MESSAGE"
    server-id: "survival-1"
  styles:
    name:
      default:
        permission: "aio.chat.style.name.def"
        color: ["FFFFFF"]          # one colour, or a gradient stop list
    message:
      default:
        permission: "aio.chat.style.message.white"
        color: ["FFFFFF"]
    chat:
      default:
        permission: "aio.chat.style.chat.def"
        format: "&d✦ &f%player% &d➜ &f%message%"
    sound:
      default:
        permission: "aio.chat.style.sound.def"
        sound: "ENTITY_ITEM_PICKUP;1;1"   # <sound-key>;<volume>;<pitch>, empty = silent