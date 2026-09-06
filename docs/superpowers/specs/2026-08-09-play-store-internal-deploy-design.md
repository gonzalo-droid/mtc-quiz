# Play Store Internal Testing Deploy — Design

> **Updated 2026-09-06.** Originally written 2026-08-09, before PR #10, #12 and #13 changed the
> signing config, the Fastfile and the deploy workflow. Sections marked **[updated]** reflect the
> current state of `master`; the original risk analysis below them still stands.

## Goal

Get the manual CD pipeline (GitHub Actions workflow + Fastlane lane, both already merged in [PR #3](https://github.com/gonzalo-droid/mtc-quiz/pull/3)) working end-to-end: a maintainer clicks "Run workflow" and a signed release App Bundle lands on Google Play's **Internal Testing** track, without ever running automatically on a push or PR.

Because the pipeline has never executed, the **first** run should be a dry run that validates against Play without publishing. See "Dry-run mode" below.

## Background **[updated]**

Everything below already exists in code, merged on `master`:

- **`.github/workflows/deploy-internal.yml`** — `workflow_dispatch`-only trigger (never `push`/`pull_request` — publishing is deliberate, not a side effect of merging). It now takes a **`dry_run` boolean input, defaulting to `true`**. Steps: checkout, JDK 21, `gradle/actions/setup-gradle`, Ruby + `bundle install`, decode `google-services.json`/keystore/service-account from secrets, then `bundle exec fastlane android internal dry_run:<input>` with signing env vars set.
- **`fastlane/Fastfile`** — the `internal` lane is now the *only* lane (PR #12 removed the unused `test`/`beta`/`deploy` scaffolding). It runs `gradle(task: "clean bundleRelease")` then `upload_to_play_store(track: "internal", validate_only: <from dry_run>, json_key: ENV["SUPPLY_JSON_KEY"] || "<local Appfile path>")`. **Omitting `dry_run` validates rather than publishes** — publishing requires `dry_run:false` explicitly.
- **`fastlane/Appfile`** — `package_name("com.gondroid.mtcquiz")` plus a local `json_key_file(...)`. The path was corrected in PR #13: the service account lives at `/Volumes/Neko/AndroidStudioProjects/sign/quizzmtc-22c8303d73b2.json` (the original `/Users/gonzalo/...` path no longer exists).
- **`app/build.gradle.kts`** signing config — reads the four `MTC_*` values from `System.getenv(...)` first, `project.findProperty(...)` as fallback. Since PR #10 they are read as **nullable**, and the release signing config is only created when all four are present; before that, a missing value crashed Gradle's configuration phase for *every* task, CI included.

**What's missing:** the 6 GitHub Actions repo secrets. Claude Code cannot create them (blocked by the harness's auto-mode classifier — confirmed when attempting `gh secret set`). Every value has to come from Gonzalo directly.

## Dry-run mode **[added 2026-09-06]**

`upload_to_play_store` accepts `validate_only`, which uploads the bundle to the Play Developer API, opens an edit, validates credentials, permissions and `versionCode`, and then **discards the edit instead of committing it**. Nothing reaches testers.

This exercises every failure mode in "Other realistic failure modes" below except the final publish itself, which makes it the correct first execution for a pipeline that has never run. The workflow input defaults to `true` so the safe path requires no thought; publishing requires unchecking the box (or `dry_run:false` on the CLI).

## Required secrets (exact names, already referenced by the workflow) **[updated: real paths]**

| Secret | Content | Source |
|---|---|---|
| `MTC_KEYSTORE_BASE64` | `base64 < /Volumes/Neko/AndroidStudioProjects/keys/mtcquizkeys` | The real release keystore (2554 bytes, confirmed present) |
| `MTC_KEYSTORE_PASSWORD` | plain text | In `~/.gradle/gradle.properties` |
| `MTC_KEY_ALIAS` | plain text | In `~/.gradle/gradle.properties` (`mtcquiz`) |
| `MTC_KEY_PASSWORD` | plain text | In `~/.gradle/gradle.properties` |
| `GOOGLE_SERVICES_JSON` | `base64 < app/google-services.json` | The **real** file (gitignored) — verified by hash that it is *not* `ci/google-services.dummy.json` |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | raw JSON content | `/Volumes/Neko/AndroidStudioProjects/sign/quizzmtc-22c8303d73b2.json` |

## Already verified **[added 2026-09-06]**

- **The Gradle half of the pipeline works.** `./gradlew bundleRelease` was run locally with the real credentials: BUILD SUCCESSFUL, a 28.9 MB AAB, and `jarsigner -verify` reports *jar verified* with `META-INF/MTCQUIZ.RSA`. Whatever fails on a first run, it will not be the build or the signing.
- **Google Play Android Developer API is enabled** (Task 1 Step 3, confirmed 2026-09-06).
- **`versionCode` is 8** / `versionName` 1.2.3, bumped in `ae0e0ba` — no longer the `"7"` this spec originally described.

## Known risk: Play Console can't bootstrap a brand-new app via API

Google's Play Developer API (which `upload_to_play_store`/`supply` uses) **cannot create a new app or upload its first-ever build** — at least one release must already have been uploaded manually through the Play Console UI before any API-driven upload (including this pipeline) will succeed. Since `fastlane/Appfile` already points at a real service-account file used for local releases, this app almost certainly already has a manual release on Play Console — but this has not been confirmed, and is the single most likely reason a first run of this workflow could fail with a permissions/not-found-style error unrelated to anything in this repo's code.

Source: [fastlane `upload_to_play_store` docs](https://docs.fastlane.tools/actions/upload_to_play_store/)

## Other realistic failure modes

- **Service account lacks permission on this app.** In Play Console → Users and permissions, the service account's email (visible inside the JSON key as `client_email`) needs at least "Release manager" access to `com.gondroid.mtcquiz`. Surfaces as an HTTP 403 naming the missing permission. *(The other half of this risk — the Play Developer API not being enabled — is now resolved.)*
- **`versionCode` collision.** `gradle/libs.versions.toml`'s `projectVersionCode` is a hardcoded, manually-bumped string (currently `"8"`). Play Console rejects an upload whose `versionCode` was already used on any track. A dry run catches this without burning the number.
- **AAB vs APK.** The `internal` lane uses `bundleRelease` (AAB) — the format Play Console expects for anything not grandfathered onto APK uploads. If the app has only ever published APKs, confirm in Play Console → Release → Setup → App integrity that App Bundle uploads are accepted (they are for any app not explicitly opted out).
- **Bundler platform mismatch. [added 2026-09-06]** `Gemfile.lock` lists only `arm64-darwin-23` and `ruby`. If `bundle install` complains about the platform on the Linux runner, fix with `bundle lock --add-platform x86_64-linux` and commit the lockfile. Untested — the local Ruby install is broken, so this could not be reproduced ahead of time.

## Definition of done **[updated]**

1. All 6 secrets exist in the repo (`gh secret list` shows them — values are never visible again once set, only names).
2. A maintainer manually runs the workflow **with `dry_run` checked (the default)**, and it completes green — proving credentials, permissions and `versionCode` are all accepted by Play.
3. The maintainer runs it again with `dry_run` unchecked.
4. The run completes with a green checkmark.
5. The new build is visible in Play Console → Testing → Internal testing, with a `versionCode` matching what was built.

## Out of scope

- Automating the *trigger* (e.g., on a git tag) — deliberately manual for now; revisit only after at least one successful manual run.
- Automated `versionCode`/`versionName` bumping — still manual, still a future improvement.
- ~~The `beta` (Crashlytics) and `deploy` lanes~~ — removed entirely in PR #12; `internal` is the only lane left.
