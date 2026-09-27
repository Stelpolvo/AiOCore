# AiOCore

> A personal, all-in-one Bukkit plugin for Minecraft **1.18 through the latest version**.

AiOCore is designed to be a lightweight, modular foundation for survival and
small-to-medium servers. Instead of installing a pile of separate plugins,
AiOCore aims to provide a unified set of core features under one roof.

## ✨ Current Features

### 💰 Multi-Currency Economy
- Define unlimited currencies in `settings.yml` (e.g. `金币`, `点券`)
- Fully compatible with **Vault** — any Vault-aware plugin can use it
- Player-to-player transfer with per-currency toggles
- Admin commands: `get` / `set` / `give` / `take`
- Per-currency formatting, symbols, and decimal precision
- Persistent player data with configurable auto-save interval

### 💬 Chat System
- **Channels** — define unlimited chat channels, each with its own permission and prefix
  - Per-player channel selection, persisted across sessions
  - Tab-completion for channel names
- **Custom Styles** — four independent style categories that can be freely combined:
  - **Name style** — gradient colors for player names
  - **Message style** — gradient colors for message content
  - **Chat style** — layout template (`%player%`, `%message%`, `%server%` placeholders)
  - **Sound style** — per-player sound feedback on chat (`key;volume;pitch`)
- **Cross-server Chat** — broadcast messages across servers via BungeeCord / Velocity
  - `PLUGIN_MESSAGE` forwarding (no proxy plugin required)
  - Echo filtering — a server's own messages are not re-broadcast locally
- **Mute Toggle** — `/aio chat mute` to toggle chat sound on/off
- **Style Preview** — `/aio chat <type> show` to preview all available styles

### 🔌 PlaceholderAPI Support
- `%aio_economy_<currency>%`, `..._formatted%`, `..._display%`, `..._symbol%`, `..._name%`
- `%aio_economy_<currency>_player_<name>%` for querying other players

### 🗄️ Flexible Storage
- **YAML** — simple flat-file storage, good for small servers
- **SQLite** — single-file database, zero-config
- **MySQL** — for networks and multi-server setups
- All storage types share the same interface; switch by editing one config line

### ⌨️ Command Framework
- Unified `/aio` root command with subcommand routing
- Permission-aware tab completion
- Console-friendly where applicable

## 📋 Commands

### Economy

| Command | Permission | Description |
| --- | --- | --- |
| `/aio economy pay <player> <currency> <amount>` | `aio.def.economy.pay` | Transfer currency to another player |
| `/aio economy look <currency>` | `aio.def.economy.look` | Show your own balance |
| `/aio economy get <player> <currency>` | `aio.admin.economy.get` | Query another player's balance |
| `/aio economy set <player> <currency> <amount>` | `aio.admin.economy.set` | Set a player's balance |
| `/aio economy give <player> <currency> <amount>` | `aio.admin.economy.give` | Add currency to a player |
| `/aio economy take <player> <currency> <amount>` | `aio.admin.economy.take` | Remove currency from a player |

### Chat

| Command | Permission | Description |
| --- | --- | --- |
| `/aio chat <type> show` | `aio.command.def.chat.<type>` | Preview all styles of a category |
| `/aio chat <type> set <key> [player]` | `aio.command.def.chat.<type>` | Apply a style |
| `/aio chat mute` | `aio.command.def.chat.mute` | Toggle chat sound on / off |

`<type>` can be one of: `name`, `message`, `chat`, `sound`, `channel`.

## ⚙️ Configuration

`settings.yml` controls everything. A minimal example:

```yaml
settings:
  save-interval-seconds: 2
  storage:
    type: "yml"           # yml / sqlite / mysql
    url: "/data"
    pool-size: 10
    username: "admin"
    password: "admin"

economy:
  enabled: true
  vault: "金币"
  currencies:
    金币:
      display-name: "金币"
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
        color: ["FFFFFF"]
    message:
      default:
        permission: "aio.chat.style.message.def"
        color: ["AA00AA", "00FFFF"]
    chat:
      default:
        permission: "aio.chat.style.chat.def"
        format: "&d✦ &f%player% &d➜ &f%message%"
    sound:
      default:
        permission: "aio.chat.style.sound.def"
        sound: "ENTITY_ITEM_PICKUP;1;1"
```

### Storage Backends

Set `settings.storage.type` to one of:

- `yml` — flat-file storage under `settings.storage.url`
- `sqlite` — single-file DB; set `url` to a JDBC URL like `jdbc:sqlite:plugins/AiOCore/data.db`
- `mysql` — set `url` to a MySQL JDBC URL; `username` / `password` are required

For MySQL, the recommended JDBC URL parameters:

```
jdbc:mysql://127.0.0.1:3306/aio?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&rewriteBatchedStatements=true
```

## 📦 Requirements

| Requirement    | Version                     |
| -------------- | --------------------------- |
| Server         | Paper / Bukkit **1.18+**    |
| Java           | 17+                         |
| Vault          | Required for economy        |
| PlaceholderAPI | Optional (for placeholders) |

## 🔧 Building

```bash
mvn clean package
```

The output jar will be placed in `target/`. The bundled SQLite and MySQL
drivers are declared via `plugin.yml`'s `libraries` section, so the server
will download them on first run — no need to shade them into the jar.

## 🚧 Roadmap

AiOCore is under active personal development. Planned modules:

- [x] Multi-currency economy
- [x] Chat system with styles, channels and cross-server support
- [ ] **Menus / GUIs** — a declarative, config-driven menu system
- [ ] Additional PlaceholderAPI expansions
- [ ] Redis-based cross-server transport

## 📄 License

See [LICENSE](LICENSE).

