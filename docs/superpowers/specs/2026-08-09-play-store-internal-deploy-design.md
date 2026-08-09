# Play Store Internal Testing Deploy — Design

## Goal

Get the manual CD pipeline (GitHub Actions workflow + Fastlane lane, both already merged in [PR #3](https://github.com/gonzalo-droid/mtc-quiz/pull/3)) working end-to-end: a maintainer clicks "Run workflow" and a signed release App Bundle lands on Google Play's **Internal Testing** track, without ever running automatically on a push or PR.

## Background

Everything below already exists in code, merged on `master`:

- **`.github/workflows/deploy-internal.yml`** — `workflow_dispatch`-only trigger (never `push`/`pull_request` — publishing is deliberate, not a side effect of merging). Steps: checkout, JDK 21 + Gradle cache, Ruby + Fastlane, decode `google-services.json`/keystore/service-account from secrets, `chmod +x gradlew`, run `fastlane android internal` with signing env vars set.
- **`fastlane/Fastfile`** — `internal` lane: `gradle(task: "clean bundleRelease")` then `upload_to_play_store(track: "internal", json_key: ENV["SUPPLY_JSON_KEY"] || "<local Appfile path>")`. The `json_key` fallback means `fastlane android internal` still works unchanged for a local manual release using the maintainer's existing local credentials.
- **`fastlane/Appfile`** — already has `package_name("com.gondroid.mtcquiz")` and a local `json_key_file(...)` path, confirming a Play Console service account already exists and is used for local releases today.
- **`app/build.gradle.kts`** signing config — reads `MTC_KEYSTORE_PATH`/`MTC_KEYSTORE_PASSWORD`/`MTC_KEY_ALIAS`/`MTC_KEY_PASSWORD` from `System.getenv(...)` first, `project.findProperty(...)` as fallback (used for local `gradle.properties`-based releases).

**What's missing:** this pipeline has never actually run, because it needs 6 GitHub Actions repo secrets that don't exist yet, and Claude Code cannot create repo secrets itself (blocked by the harness's auto-mode classifier — confirmed when attempting `gh secret set` earlier in this project). Every value has to come from Gonzalo directly.

## Required secrets (exact names, already referenced by the workflow)

| Secret | Content | Source |
|---|---|---|
| `MTC_KEYSTORE_BASE64` | `base64 < path/to/keystore.jks` | The real release keystore already used for local releases |
| `MTC_KEYSTORE_PASSWORD` | plain text | Keystore password |
| `MTC_KEY_ALIAS` | plain text | Signing key alias inside the keystore |
| `MTC_KEY_PASSWORD` | plain text | Signing key password (often same as keystore password) |
| `GOOGLE_SERVICES_JSON` | `base64 < app/google-services.json` | The **real** file (gitignored) — not `ci/google-services.dummy.json`, which is CI-only and has fake Firebase project data |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | raw JSON content | The file already referenced in `fastlane/Appfile`'s `json_key_file(...)` |

## Known risk: Play Console can't bootstrap a brand-new app via API

Google's Play Developer API (which `upload_to_play_store`/`supply` uses) **cannot create a new app or upload its first-ever build** — at least one release must already have been uploaded manually through the Play Console UI before any API-driven upload (including this pipeline) will succeed. Since `fastlane/Appfile` already points at a real service-account file used for local releases, this app almost certainly already has a manual release on Play Console — but this has not been confirmed, and is the single most likely reason a first run of this workflow could fail with a permissions/not-found-style error unrelated to anything in this repo's code.

Source: [fastlane `upload_to_play_store` docs](https://docs.fastlane.tools/actions/upload_to_play_store/)

## Other realistic failure modes

- **Service account lacks permission on this app.** In Play Console → Users and permissions, the service account's email (visible inside the JSON key as `client_email`) needs at least "Release manager" access to `com.gondroid.mtcquiz`, plus the Play Developer API must be enabled for the linked Google Cloud project. `upload_to_play_store` surfaces this as an HTTP 403 with a message naming the missing permission.
- **`versionCode` collision.** `gradle/libs.versions.toml`'s `projectVersionCode` is a hardcoded, manually-bumped string (currently `"7"` as of this writing). Play Console rejects an upload whose `versionCode` was already used on any track. If the last manual release already used the current value, bump `projectVersionCode` before triggering the workflow.
- **AAB vs APK.** The existing `deploy`/`beta` lanes use `assembleRelease` (APK); the new `internal` lane deliberately uses `bundleRelease` (AAB) — the format Play Console expects for anything not already grandfathered onto APK uploads. If the app has only ever published APKs, first confirm in Play Console → Release → Setup → App integrity that App Bundle uploads are accepted (they are for any app not explicitly opted out).

## Definition of done

1. All 6 secrets exist in the repo (`gh secret list` shows them — values are never visible again once set, only names).
2. A maintainer manually runs the workflow from the Actions tab.
3. The run completes with a green checkmark.
4. The new build is visible in Play Console → Testing → Internal testing, with a `versionCode` matching what was built.

## Out of scope

- Automating the *trigger* (e.g., on a git tag) — deliberately manual for now; revisit only after at least one successful manual run.
- Automated `versionCode`/`versionName` bumping — flagged as a future improvement in an earlier conversation, not required for this pipeline to work once.
- The `beta` (Crashlytics) and `deploy` (original, APK-based Play Store) Fastlane lanes — untouched, still local-only, out of scope for this spec.
