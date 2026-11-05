# Hybrid Solana Trading Engine — Implementation Status

## Current safety boundary

This branch is a **paper-trading-only foundation**. It does not connect to Solana RPC, derive real wallet addresses, sign transactions, submit swaps, or send withdrawals.

The market feed now reads public Solana DEX pair data from DexScreener. Prices, 5-minute/1-hour changes, market cap, 24-hour volume, liquidity, pair creation time and available social links come from the API. Trading remains local paper simulation only. Holder counts, bonding-curve progress, OHLC history, token authorities, holder concentration, LP lock and honeypot checks are not inferred from DEX pair data.

## Phase 0 — Security and core refactoring

- [x] Disable hash-based pseudo Solana public-key derivation.
- [x] Disable random pseudo transaction signatures.
- [x] Reject wallet import until audited Ed25519 derivation and secure key storage are integrated.
- [x] Reject withdrawals until a real signed transaction pipeline exists.
- [x] Default the bot master switch to OFF.
- [x] Label the bot status as paper engine and disclose simulated market data.
- [x] Replace tests that previously treated fake derivation/withdrawals as successful.
- [x] Run unit tests and build the debug APK in GitHub Actions on main (2026-11-05).

## Phase 1 — Live market data foundation

- [x] Add a read-only DexScreener data source for latest Solana token profiles and pair snapshots.
- [x] Resolve the wrapped-SOL/USDC or wrapped-SOL/USDT pair for a live SOL/USD reference price.
- [x] Map actual mint addresses, pair prices, 5m/1h changes, market cap/FDV, 24h volume, liquidity, pair creation time and available social links.
- [x] Refresh market snapshots every 30 seconds and expose LIVE, STALE, ERROR and CONNECTING states.
- [x] Preserve the last good snapshot if a refresh fails.
- [x] Remove random price ticks and fabricated seed tokens/candles from the active market feed.
- [x] Mark missing security/bonding-curve fields as unverified instead of manufacturing a score or progress value.
- [x] Keep paper trading isolated; no wallet signing, RPC swap submission or withdrawal is enabled.
- [ ] Validate the Phase 1 branch with GitHub Actions and verify the live API response on a device.

### Phase 1 data limitations

DexScreener is a market/pair data source, not a token security oracle. The current adapter does not claim to verify mint/freeze authorities, holder concentration, LP locks, honeypot behavior, Pump.fun bonding progress, holder counts or historical OHLC candles. Unverified security checks display N/A; the app must not classify these tokens as safe based on missing data.
## Phase 2 — Smart auto-trading foundation (paper only)

- [x] Add deterministic candidate scoring using pair liquidity, 24h volume, 5m/1h momentum, pair age and market-cap range.
- [x] Add explicit eligibility blockers and default rejection when on-chain security data is unverified.
- [x] Add configurable 0.01 SOL default paper entry size, candidate score threshold, liquidity/volume floors and maximum open positions.
- [x] Add paper auto-buy with duplicate-entry prevention and balance checks.
- [x] Add paper auto-sell using stop-loss, take-profit and trailing-stop rules.
- [x] Add a daily realized-loss gate that blocks new buys but does not disable protective exits.
- [x] Add candidate score/status UI and controls for enabling auto-buy/auto-sell.
- [x] Add unit tests for default risk gates and exit decisions.
- [ ] Validate the smart auto-trading branch with CI and run extended paper-trading tests.

### Strategy defaults

- Entry size: 0.01 SOL per token.
- Candidate score: at least 75/100.
- Minimum liquidity: USD 20,000; minimum 24-hour volume: USD 50,000.
- Market-cap range: USD 10,000 to USD 2,000,000; pair age up to 48 hours.
- Require positive 5-minute and 1-hour momentum.
- Maximum open positions: 3; daily realized loss cap: 0.05 SOL.
- Paper exits: take profit +100%, stop loss -20%, trailing stop 15 percentage points below peak PnL.
- Auto-buy is OFF by default. Unverified security data is blocked by default.

Scores are ranking heuristics, not predictive probabilities. No 500x outcome is promised or inferable from these public market fields. The current DEX data adapter does not verify authorities, holder concentration, honeypot behavior or LP locks. Enabling purchases while those checks are unverified is a deliberate high-risk override. All orders in this phase are paper simulations and do not create on-chain transactions.
## Required architecture before live trading

1. **Market data adapters** — real Solana RPC/WebSocket plus verified token and pool data. Mock data must remain isolated.
2. **Strategy engine** — sniper, momentum, limit, take-profit, stop-loss, trailing-stop and partial-exit strategies emit signals only.
3. **Risk engine** — position sizing, max exposure, daily loss, liquidity, slippage, price impact, token authority and holder concentration gates.
4. **Execution router** — Paper executor and Live executor are separate implementations behind one interface. Strategies must not call wallet/RPC methods directly.
5. **Wallet boundary** — audited Solana Ed25519/keypair implementation, Android Keystore-backed encryption where applicable, explicit signing consent and no logging of secrets.
6. **Transaction lifecycle** — build, simulate, sign, submit, confirm, reconcile balances and persist final status. A submitted RPC request is not a confirmed trade.
7. **Persistence and recovery** — Room-backed orders, fills, positions and transaction states; idempotent recovery after process death.

## Live-trading release gates

Live mode must remain unavailable until all of these pass:

- Derived public key matches a known Solana keypair test vector.
- Transaction signatures verify against the expected public key.
- Devnet end-to-end transfer and swap tests pass.
- Mainnet market data is independently verified.
- Paper and live balances/positions are isolated.
- Failed, expired, or unconfirmed transactions never become FILLED.
- Slippage, priority fee, price impact and max-loss limits are enforced.
- Stop-loss and take-profit exits are idempotent and support partial amounts.
- Secret material never enters logs, analytics, crash reports or source control.

## Important

Do not enter a seed phrase or private key into the current app. Do not treat the displayed sample balances, tokens, prices, PnL, alerts or signatures as real assets or blockchain activity.
