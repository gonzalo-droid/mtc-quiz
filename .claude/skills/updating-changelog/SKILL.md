---
name: updating-changelog
description: Use when notable changes (features, fixes, dependency bumps, architecture changes) have landed on master and CHANGELOG.md's Unreleased section needs updating, or when cutting a new version entry for a release
---

# Updating the Changelog

## Overview

Keep `CHANGELOG.md` an accurate, skimmable record of *what shipped and why*, in the project's established Keep-a-Changelog-derived format — not a git-log dump.

## When to Use

- After a PR/commit lands on `master` that a user or future maintainer would care about (feature, fix, dependency bump, architecture change, removal).
- When cutting a release: `projectVersionCode`/`projectVersionName` are about to bump in `gradle/libs.versions.toml`.

**Skip for:** pure refactors with no external effect, formatting-only commits, typo fixes, test-only additions that don't reflect a behavior change worth calling out — unless they were notable enough that a `## [Sin publicar]` reader would want to know (e.g. "ktlint running in CI for the first time" *was* included because it changes what contributors experience).

## Source of Truth

Read `git log` (and `git diff`/`git show` on ambiguous commits) since the last changelog entry — not chat history, not memory of what was discussed. Compare against what's already listed under `## [Sin publicar]` to avoid duplicating an entry.

```bash
git log --oneline <last-entry-commit-or-date>..HEAD
```

## Workflow

### Adding to the unreleased section

1. Find or create `## [Sin publicar]` at the very top of the file, right after the intro paragraph. If it doesn't exist yet, add it with a one-line note on what version bump is pending (see existing entries for phrasing, e.g. *"Cambios ya en `master` pero pendientes del próximo bump de versión (`versionCode` 7 → 8)."*).
2. File each change under the right subsection, creating it if missing (order: `### Added`, `### Changed`, `### Fixed`, `### Removed`) — omit subsections with nothing in them.
3. Write each entry as a single bullet, in Spanish, matching the existing voice:
   - Cite the specific type/file/function in backticks when it anchors the reader (`BannerAdSlot`, `PremiumRepositoryImpl.loadAvailablePlans`).
   - State *why* only when it's not obvious from the *what* (a deadline, a Google Play requirement, a bug's user-visible symptom) — see `Google Play Billing Library 7.1.1 → 9.1.0 (requisito de Google Play, deadline 2026-08-30)` for the pattern.
   - One line per entry. If a change needs more than that to explain, it's probably two entries.

### Cutting a release

When `projectVersionCode`/`projectVersionName` bump:
1. Rename `## [Sin publicar]` to `## [<versionName>] - <YYYY-MM-DD> (versionCode <N>)`, drop the "pendiente" note line.
2. Add a fresh empty `## [Sin publicar]` above it for the next round.

## Quick Reference

| Section | Use for |
|---|---|
| `### Added` | New features, screens, capabilities |
| `### Changed` | Behavior/architecture changes to existing things |
| `### Fixed` | Bug fixes |
| `### Removed` | Deleted features, dead code, superseded components |

## Common Mistakes

- **Copying commit messages verbatim.** Commit messages explain the change to a reviewer mid-diff; changelog entries explain it to someone who never saw the diff. Rewrite for that reader.
- **Listing every commit.** Squash related commits (e.g. a feature + its follow-up fix) into one entry if that's how a reader would want to learn about it.
- **Forgetting the "why".** A bare `Fixed: BannerAdSlot padding` is less useful than one line on what was visibly broken.
- **English.** This changelog is Spanish — match it.
