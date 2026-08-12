# Play Store Internal Testing Deploy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Human-in-the-loop notice:** Most tasks below require Gonzalo directly — they touch real production credentials (release keystore, Play Console service account) and an external system (GitHub repo secrets, Google Play Console) that Claude Code is not permitted to modify on its own (confirmed blocked by the harness's auto-mode classifier when `gh secret set` was attempted). An agentic worker executing this plan should perform the verification/read-only steps and the code step (Task 4, if needed), but must stop and hand Task 1–3's action items back to Gonzalo rather than attempting to obtain or enter credentials itself.

**Goal:** Make the already-merged `deploy-internal.yml` workflow + Fastlane `internal` lane actually succeed end-to-end: manually triggered, produces a signed AAB, uploads it to Google Play's Internal Testing track.

**Architecture:** No new code by default — this plan is primarily operational (Play Console verification, GitHub secret creation, a manual workflow run). Task 4 is a contingency: concrete code fixes for the specific failure modes identified in the design spec, included so a first failed run doesn't stall on "now what."

**Tech Stack:** GitHub Actions, Fastlane (`supply`/`upload_to_play_store`), Gradle (`bundleRelease`), Google Play Console.

## Global Constraints

- Never make this workflow trigger automatically (no `push`/`pull_request` triggers) — publishing must stay a deliberate, manual action. Do not change `on:` in `.github/workflows/deploy-internal.yml` as part of this plan.
- Never print, log, or paste the contents of any secret value in chat, commit messages, or workflow logs. All commands below write secret values directly from local files/env into `gh secret set`, never through an intermediate visible step.
- Design spec (full rationale): `docs/superpowers/specs/2026-08-09-play-store-internal-deploy-design.md`.

---

### Task 1: Verify Play Console prerequisites

**Files:** None — read-only checks in Google Play Console and Google Cloud Console web UIs.

**Interfaces:** None.

- [ ] **Step 1: Confirm the app has at least one manually-uploaded release**

In [Play Console](https://play.google.com/console) → select the `com.gondroid.mtcquiz` app → Release → Production (or any track) → Releases. Confirm at least one release exists. If none exists anywhere (all tracks empty), the Play Developer API cannot bootstrap the app — a release must be uploaded manually through this UI first, before Task 3 can succeed. Given `fastlane/Appfile` already references a service-account file used for local releases, this step is very likely already satisfied — this check exists to rule it out definitively before troubleshooting anything else.

- [ ] **Step 2: Confirm the service account has permission on this app**

Open the JSON file referenced in `fastlane/Appfile`'s `json_key_file(...)` (locally, not in chat) and note its `"client_email"` field. In Play Console → Users and permissions, confirm that email is listed with at least "Release manager" access (or Admin) scoped to `com.gondroid.mtcquiz`. If it's missing, add it with Release manager access before Task 3.

- [ ] **Step 3: Confirm the Play Developer API is enabled**

In [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Library, search "Google Play Android Developer API", confirm it shows "API enabled" for the project tied to the service account above. Enable it if not.

- [ ] **Step 4: Check for a `versionCode` collision**

Read `gradle/libs.versions.toml`, find `projectVersionCode`. Compare against the highest `versionCode` already used on any Play Console track (visible in each release's details). If they match, this exact `versionCode` was already published — bump `projectVersionCode` (and `projectVersionName` to match) in `gradle/libs.versions.toml` before Task 3, commit that change on its own:

```bash
git add gradle/libs.versions.toml
git commit -m "chore: bump versionCode for internal testing release"
git push
```

---

### Task 2: Create the 6 GitHub repo secrets

**Files:** None — GitHub repo secrets, external to the git tree.

**Interfaces:** Consumes nothing from Task 1 except its pass/fail outcome (don't proceed if Task 1 found a blocking issue). Produces the 6 secret names `.github/workflows/deploy-internal.yml` already references by name — no workflow file change needed, since it already reads exactly these names.

- [ ] **Step 1: Set the keystore secrets**

```bash
gh secret set MTC_KEYSTORE_BASE64 --body "$(base64 < /path/to/your/release.jks)" --repo gonzalo-droid/mtc-quiz
gh secret set MTC_KEYSTORE_PASSWORD --body "YOUR_KEYSTORE_PASSWORD" --repo gonzalo-droid/mtc-quiz
gh secret set MTC_KEY_ALIAS --body "YOUR_KEY_ALIAS" --repo gonzalo-droid/mtc-quiz
gh secret set MTC_KEY_PASSWORD --body "YOUR_KEY_PASSWORD" --repo gonzalo-droid/mtc-quiz
```

- [ ] **Step 2: Set the real `google-services.json`**

Run from the repo root, using the real (gitignored) file — not `ci/google-services.dummy.json`:

```bash
gh secret set GOOGLE_SERVICES_JSON --body "$(base64 < app/google-services.json)" --repo gonzalo-droid/mtc-quiz
```

- [ ] **Step 3: Set the Play Store service account key**

Using the same file whose `client_email` was checked in Task 1 Step 2:

```bash
gh secret set PLAY_STORE_SERVICE_ACCOUNT_JSON --body "$(cat /path/to/quizzmtc-22c8303d73b2.json)" --repo gonzalo-droid/mtc-quiz
```

- [ ] **Step 4: Verify all 6 secrets exist (names only, values are never retrievable)**

```bash
gh api repos/gonzalo-droid/mtc-quiz/actions/secrets --jq '.secrets[].name'
```

Expected output: exactly these 6 names (order may vary) — `MTC_KEYSTORE_BASE64`, `MTC_KEYSTORE_PASSWORD`, `MTC_KEY_ALIAS`, `MTC_KEY_PASSWORD`, `GOOGLE_SERVICES_JSON`, `PLAY_STORE_SERVICE_ACCOUNT_JSON`.

---

### Task 3: Trigger and verify the first deploy run

**Files:** None.

**Interfaces:** Consumes the 6 secrets from Task 2.

- [ ] **Step 1: Trigger the workflow manually**

Either via GitHub UI (Actions tab → "Deploy to Play Store (Internal Testing)" → Run workflow → select `master` → Run workflow), or via CLI:

```bash
gh workflow run deploy-internal.yml --repo gonzalo-droid/mtc-quiz --ref master
```

- [ ] **Step 2: Watch the run**

```bash
gh run list --workflow=deploy-internal.yml --repo gonzalo-droid/mtc-quiz --limit 1
gh run watch --repo gonzalo-droid/mtc-quiz
```

Expected: all steps green, ending with `fastlane android internal` succeeding.

- [ ] **Step 3: If it fails, capture the log before doing anything else**

```bash
gh run view --repo gonzalo-droid/mtc-quiz --log-failed
```

Match the error against Task 4's failure modes before attempting a fix — each maps to a specific, known cause; guessing at a fix without matching the error first risks masking the real problem.

- [ ] **Step 4: If it succeeds, confirm in Play Console**

Play Console → `com.gondroid.mtcquiz` → Testing → Internal testing → Releases. Confirm the new release appears with the expected `versionCode`/`versionName`.

---

### Task 4: Contingency — fix the specific failure, if Task 3 failed

**Files:**
- Modify (only if the matching symptom below appears): `fastlane/Fastfile:39-46` (the `internal` lane)

**Interfaces:** None — this task only runs if Task 3 Step 3's log matches one of these exact symptoms. Do not apply a fix speculatively; match the log first.

- [ ] **Step 1: Match the failure log against known symptoms**

| Log contains | Cause | Fix |
|---|---|---|
| `The caller does not have permission` / HTTP 403 | Service account lacks Play Console access, or Play Developer API not enabled | Redo Task 1 Steps 2–3; re-run Task 3 once fixed — no code change |
| `APK specifies a version code that has already been used` / `versionCodeAlreadyUsed` | `versionCode` collision | Redo Task 1 Step 4 (bump and push), then re-run Task 3 |
| `Package not found` / `applicationNotFound` | No manual release ever uploaded for this app (Task 1 Step 1 was skipped or wrong) | A release must be uploaded manually through the Play Console UI first — outside the scope of automation; do this once, then re-run Task 3 |
| `keystore was tampered with, or password was incorrect` | Wrong `MTC_KEYSTORE_PASSWORD`/`MTC_KEY_PASSWORD`, or `MTC_KEYSTORE_BASE64` wasn't valid base64 of the real file | Redo Task 2 Step 1 — a copy/paste or `base64` command error is the most common cause |
| `Malformed root json at .../google-services.json` | `GOOGLE_SERVICES_JSON` secret is empty/wrong | Redo Task 2 Step 2 — confirm `app/google-services.json` exists locally and is valid JSON (`python3 -c "import json; json.load(open('app/google-services.json'))"`) before re-encoding |
| Anything not listed above | Unclassified — do not guess | Stop and report the exact log lines back to Gonzalo instead of attempting a fix |

- [ ] **Step 2: If the symptom matched has no code fix (permission/versionCode/no-release/secret rows above), stop here**

Re-run Task 3 after the underlying (non-code) cause is fixed. No commit needed.

- [ ] **Step 3: Re-run Task 3 after any fix**

Repeat Task 3 Steps 1–4 in full — do not assume success; verify the run and Play Console state again.

## Self-Review Notes

- **Spec coverage:** every requirement in the design spec — required secrets (Task 2), the "can't bootstrap a new app" risk (Task 1 Step 1, Task 4 row 3), service-account-permission risk (Task 1 Step 2, Task 4 row 1), `versionCode` collision risk (Task 1 Step 4, Task 4 row 2), and the definition-of-done checklist (Task 3 Steps 2 and 4) — is covered by a task above.
- **Placeholder scan:** the only non-literal values are secret contents themselves (`YOUR_KEYSTORE_PASSWORD`, file paths) — these are inherently per-machine/per-credential and cannot be hardcoded into a shared plan; every other step has an exact, runnable command.
- **Type consistency:** the 6 secret names in Task 2 Step 4's expected output match exactly what `.github/workflows/deploy-internal.yml` (already merged) reads via `${{ secrets.* }}`, and match the table in the design spec.
