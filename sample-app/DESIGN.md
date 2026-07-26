# Sample App Design Direction

## Visual Language

- One token source: the app chrome and the rendered document both come from the active `OrcaPalette`.
- Flat warm-neutral surfaces with one restrained accent; a high-contrast palette is one tap away.
- No gradients, shadows, elevation overlays, floating cards, or decorative effects — anywhere.
- Dividers and one-pixel outlines provide structure; every control keeps a visible outline.
- Rounded pills are reserved for navigation, filters, and compact status.

## Layout

- A command bar anchors every layout: brand mark, version, appearance controls, theme toggle.
- Phones use a horizontally scrollable suite switcher; appearance controls collapse behind a single
  tune button below `680.dp` and expand into their own row.
- Wide screens (`900.dp`+) use a persistent `248.dp` suite rail with numbered entries.
- Document content is centered and capped at `940.dp` for readable line lengths.
- The playground becomes a source/preview split at `760.dp` and a focused two-pane switch below it.
- Streaming controls remain inline with the document and expose pause, replay, pace, progress, and completion state.

## Controls

- Search sits directly above the document: query field, match counter, previous/next navigation.
- Measurement strips are divider-only grids; they reflow from four columns to two on narrow screens.
- Style source (`Flat`, `Contrast`, `Material`) and density (`Compact`, `Cozy`, `Roomy`) are
  switchable at runtime and affect both the shell and the renderer.

## Typography And Color

- Use hierarchy through size, weight, and muted color rather than elevation.
- Monospace uppercase labels mark metadata; body copy stays in the sans stack.
- Dark mode must preserve legible table body and header text.
- Keep labels terse: `Read`, `Tokens`, `Blocks`, `Data`, `Media`, `Math`, `More`, `API`, `Stream`, `Edit`.
