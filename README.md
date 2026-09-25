# AiOCore

> A personal, all-in-one Bukkit plugin for Minecraft **1.18 through the latest version**.

AiOCore is designed to be a lightweight, modular foundation for survival and
small-to-medium servers. Instead of installing a pile of separate plugins,
AiOCore aims to provide a unified set of core features under one roof.

## ✨ Current Features

- **Multi-Currency Economy**
  - Define unlimited currencies in `settings.yml` (e.g. `金币`, `点券`)
  - Fully compatible with **Vault** — any Vault-aware plugin can use it
  - Player-to-player transfer with per-currency toggles
  - Admin commands: `get` / `set` / `give` / `take`
  - Per-currency formatting, symbols, and decimal precision
  - Persistent player data with configurable auto-save interval
- **PlaceholderAPI Support**
  - `%aio_economy_<currency>%`, `..._formatted%`, `..._display%`, etc.
- **Command Framework**
  - Unified `/aio` root command with subcommand routing
  - Permission-aware tab completion

## 🚧 Roadmap

AiOCore is under active personal development. Planned modules:

- [ ] **Chat** — channels, formatting, filters
- [ ] **Menus / GUIs** — a declarative, config-driven menu system
- [ ] Additional economy hooks and data backends

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