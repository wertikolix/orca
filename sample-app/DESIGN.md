# Sample App Design Direction

## Visual Language

- Flat warm-neutral surfaces with one restrained teal accent.
- No gradients, shadows, floating cards, or decorative effects.
- Dividers and one-pixel outlines provide structure.
- Rounded pills are reserved for navigation and compact status.

## Layout

- A compact identity/header row anchors every layout.
- Phones use a horizontally scrollable suite switcher; wide screens use a persistent `242.dp` navigation panel.
- Document content is centered and capped at `920.dp` for readable line lengths.
- The playground becomes a source/preview split at `760.dp` and a focused two-pane switch below it.
- Streaming controls remain inline with the document and expose pause, replay, pace, progress, and completion state.

## Typography And Color

- Use hierarchy through size, weight, and muted color rather than elevation.
- Dark mode must preserve legible table body and header text.
- Keep labels terse: `Read`, `Blocks`, `Data`, `Media`, `Math`, `More`, `API`, `Stream`, `Edit`.
