# LaTeX / Math Rendering Plan

## Implementation Status (`0.12.0`)

- `orca-core` now models conservative inline and display math nodes.
- `orca-compose` now exposes `inlineMathContent` and `blockMathContent` slots with text fallback.
- `orca-math-orcex` now provides an optional Compose Multiplatform renderer backed by Orcex `0.4.0` on Android, Desktop, and supported iOS targets.
- The STIX font dependency is optional and intentionally excluded from the lightweight base artifacts.

LaTeX support should not make the base Orca renderer heavy or unsafe. The first implementation should model math syntax and let consumers opt into rendering.

## Phase 1: Syntax And Slots

- Add inline math (`$...$`) and display math (`$$...$$`) AST nodes in `orca-core`.
- Parse math conservatively so ordinary currency text and incomplete streaming fragments remain plain text.
- Add `inlineMathContent` and `blockMathContent` rendering slots in `orca-compose`.
- Show a readable source fallback when no math renderer is installed.

## Phase 2: Optional Renderer

- Keep the separate optional math integration module while tracking binary cost and renderer coverage per platform.
- Keep the base `orca-compose` artifact free from a bundled TeX engine, WebView bridge, or JavaScript runtime.
- Select a renderer only after measuring binary size, first-render latency, and formula coverage in the sample app and Fish.

## Safety And Streaming

- Treat formula source as data; do not enable arbitrary HTML or executable commands.
- Cap input length and nesting where the selected engine requires protection.
- During token streaming, render unfinished delimiters as source text and upgrade to math only after a valid close delimiter arrives.

## Sample App Follow-Up

- Maintain the dedicated `Math` document screen as a manual regression surface.
- Cover inline formulas, display equations, malformed input, dark-mode contrast, and streamed formula completion.
