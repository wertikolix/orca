# Orca Sample App

## Purpose

The sample app is a focused render lab for Orca. It should make Markdown output, streaming behavior, and editable previews easy to inspect without the app shell competing with the document.

## Principles

- Put rendered content first; controls remain quiet and compact.
- Expose real library capabilities: documents, search, statistics, tables, secure media, native math, renderer overrides, streaming, and editing.
- Demonstrate the style system honestly — the lab renders with the same tokens it documents, and never invents a visual the library cannot produce.
- Let evaluators change what matters at runtime: palette, contrast, density, and theme.
- Keep both themes readable, especially code and tables.
- Prefer a credible documentation-tool feel over decorative demo UI.
- Adapt structurally: focused tabs on phones, persistent suite navigation on wide screens, and split editing where space permits.

## Suites

`Reader`, `Design`, `Blocks`, `Tables`, `Media`, `Math`, `Extended`, `Renderers`, `Streaming`, `Playground`.

## Audience

Developers evaluating Orca for documentation, chat, and Markdown preview surfaces.
