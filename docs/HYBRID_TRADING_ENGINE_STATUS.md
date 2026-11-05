# Hybrid Solana Trading Engine — Implementation Status

## Current safety boundary

This branch is a **paper-trading-only foundation**. It does not connect to Solana RPC, derive real wallet addresses, sign transactions, submit swaps, or send withdrawals.

The existing market feed and sample tokens are generated locally for UI/demo purposes. They must not be described as live market data, and their safety scores are not evidence of on-chain token safety.

## Phase 0 — Security and core refactoring

- [x] Disable hash-based pseudo Solana public-key derivation.
- [x] Disable random pseudo transaction signatures.
- [x] Reject wallet import until audited Ed25519 derivation and secure key storage are integrated.
- [x] Reject withdrawals until a real signed transaction pipeline exists.
- [x] Default the bot master switch to OFF.
- [x] Label the bot status as paper engine and disclose simulated market data.
- [x] Replace tests that previously treated fake derivation/withdrawals as successful.
- [ ] Run CI and resolve any build/test failures.

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
