---
name: updating-technical-docs
description: Use when significant code changes (new module, changed data flow, architectural decision, dependency swap, new/removed feature) may have made ARCHITECTURE.md or README.md stale
---

# Updating Technical Docs

## Overview

Keep two documents accurate and non-overlapping: `ARCHITECTURE.md` (how the code is built, for contributors) and `README.md` (what the app is and does, for users/newcomers). `CLAUDE.md` stays as-is — it's Claude Code-specific guidance, not general documentation; don't duplicate ARCHITECTURE.md into it, cross-reference instead if needed.

## The Boundary

| Goes in `ARCHITECTURE.md` | Goes in `README.md` |
|---|---|
| Module structure, layer responsibilities | What the app does (one paragraph) |
| Data flow / a feature's lifecycle | Tech stack table (names + versions, no rationale) |
| ViewModel/state-management pattern | Setup & running instructions |
| Navigation approach | CI/CD workflows |
| DI wiring | Versioning scheme |
| Convention plugins (build-logic) | Monetization (ads/billing) — product-facing |
| Any "why we built it this way" decision | Implemented features, roadmap |

**Test:** would a contributor need this to understand *how to add code correctly*? → ARCHITECTURE.md. Would a newcomer need this to understand *what the project is or how to run it*? → README.md. If a topic has both angles (e.g. billing), put the mechanism in ARCHITECTURE.md and the user-facing behavior in README.md — don't duplicate, link between them instead.

## When to Use

- A module was added, removed, or restructured.
- A data flow, state pattern, or navigation approach changed.
- A dependency injection wiring changed.
- A feature was added, removed, or its user-facing behavior changed (README's Funcionalidades tables).
- Before believing either doc is current — check it against the actual code, don't assume prior content is still true.

## Workflow

1. **Identify what changed** — read the diff/commits, not just the final state; a rewritten section is easier to get right when you know what specifically moved.
2. **Classify by the table above.** Architectural/structural → `ARCHITECTURE.md`. Product/setup-facing → `README.md`.
3. **Update in place** — find the existing section (both docs are already organized by `##` headers matching the categories above) and edit it; don't append a new section for something that already has a home.
4. **Verify claims against code**, don't trust the old doc's wording: if a section says "X uses Y", grep for it before rewriting the surrounding prose.
5. **Cross-reference, don't duplicate.** README's setup/versioning sections may reference ARCHITECTURE.md for deep technical detail and vice versa — one sentence + a link, not a copy.

## Common Mistakes

- **Writing marketing copy into ARCHITECTURE.md.** It's for contributors — describe the mechanism, not the benefit.
- **Writing implementation detail into README's feature tables.** "Historial de evaluaciones" describes what a user sees, not that it's backed by a Room JSON column — that belongs in ARCHITECTURE.md's data-flow section.
- **Trusting stale content.** Docs drift silently; a feature description or "Estado: Propuesta" can be wrong by the time you read it — confirm against the code before repeating it.
- **New section instead of updating the existing one.** Both files are already structured by topic — a change to navigation goes in the existing `## Navegación` section, not a new one at the bottom.
