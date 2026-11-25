<div align="center">
  <h1>Avalon-Helper</h1>
  <p><strong>The isolated JVM behind real Avalon matches.</strong></p>
  <p><a href="./README.zh-CN.md">简体中文</a></p>
</div>

Avalon-Helper is the thin stdin/stdout JSON bridge between Avalon and the closed-source Rusted Warfare engine. It exists for server operators and integrators that need real simulation with a clean process boundary.

## Why Avalon-Helper

- **Keep the wire clean.** Protocol stdout carries JSON while engine output stays separate.
- **Start on the current contract.** A strict v4 handshake rejects stale protocol versions before simulation starts.
- **Reuse the engine.** Existing engine loading and ASM integration remain in the helper JVM.
- **Isolate crashes.** Engine failures stay outside Avalon's Go process instead of taking the host down with them.
- **Integrate without reimplementation.** Avalon sends simulation work and receives game events over a narrow bridge.

## Learn more

- [Avalon](https://github.com/CoreMatch/Avalon) — the server runtime that drives the helper
- [Avalon operations](https://github.com/CoreMatch/Avalon/blob/main/docs/OPERATIONS.md) — operator-facing integration guidance
- [Armorial Contract](https://github.com/CoreMatch/HA-Contract) — canonical ecosystem contracts
- [Lineage record](docs/LINEAGE-COPYCAT.md) — derivative provenance and upstream comparison

## Limits

Avalon-Helper is not a standalone server, does not open-source the game engine, and does not support arbitrary engine versions.

## License and provenance

[AGPL-3.0](LICENSE) · Derived from [RW-HPS](https://github.com/RW-HPS/RW-HPS); upstream copyright notices are retained.
