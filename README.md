# ⚔️ WobbleAuction

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-1.21-F6F6F6?style=for-the-badge)
![Vault](https://img.shields.io/badge/Vault-Economy-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-Embedded-003B57?style=for-the-badge&logo=sqlite)

A modern, production-ready **auction house plugin** for Minecraft servers running **Paper 1.21+**.

WobbleAuction provides a complete player marketplace with listing creation, GUI browsing, live purchasing, claimable sale earnings, automatic listing expiration, compact money formatting, and SQLite-backed storage.

---

## ✨ Features

### 🏪 Auction House Core
- List items directly from the main hand with `/ah sell <price>`
- Browse all active listings through a paginated GUI
- Buy items directly from the auction GUI
- View your own active listings
- Claim money earned from sold items
- Retrieve expired unsold items

---

### 💰 Economy Integration
- Full **Vault** support
- Works with EssentialsX, CMI, and similar economy plugins
- Configurable minimum / maximum price
- Configurable listing tax
- Sale earnings stored safely in claim balance

---

### 🖥️ GUI System
- Clean 54-slot auction menu
- Pagination
- Sorting:
  - Newest
  - Lowest Price
  - Highest Price
- Search by seller name or item name
- Reset filter button
- My Listings button
- Sound feedback for actions

---

### 🔍 Search & Sorting
- Search through chat input
- Supports partial matching
- Filters by seller name
- Filters by item material name
- Search works together with pagination
- Search works together with sorting

---

### 📦 Expiration System
- Listings automatically expire after configured duration
- Expired items are stored safely
- Players can reclaim expired items with `/ah expired`
- Periodic expiration checks handled automatically

---

### 📊 Money Formatting
Compact number formatting is used for auction prices:
- `1,000 → 1K`
- `1,000,000 → 1M`
- `1,000,000,000 → 1B`

---

### 💾 Database
- Embedded SQLite
- No external database required
- Automatic table creation
- Stores:
  - auction listings
  - claim balances
  - expired items
  - sale history

---

## ⚙️ Commands

| Command | Description |
|--------|-------------|
| `/ah` | Open auction GUI |
| `/ah sell <price>` | List the item in your hand |
| `/ah my` | View your active listings |
| `/ah claim` | Claim money from sold items |
| `/ah expired` | Return expired items |
| `/ah reload` | Reload configuration |

---

## 🔐 Permissions

| Permission | Description |
|-----------|------------|
| `wobble.auction.admin` | Full admin access |
| `wobble.auction.reload` | Reload plugin |
| `wobble.auction.limit.default` | Default listing limit |
| `wobble.auction.limit.vip` | VIP listing limit |

---

## 📦 Installation

1. Download the latest `.jar`
2. Place it into `/plugins`
3. Install dependencies:
   - Vault
   - Economy plugin (EssentialsX / CMI / similar)
4. Restart server

---

## 📁 Configuration

### `config.yml`

```yml
auction:
  min-price: 100.0
  max-price: 1000000000.0
  tax-percent: 5.0
  listing-duration-hours: 24
  check-interval-seconds: 60
  max-active-listings-default: 3