# Repository Guidelines

## Project Structure and Module Organization

This repository is a monorepo. Keep product assets, shared code, and application
entry points separated:

- `docs/planning/` contains the product brief, screen and flow definition,
  roadmap, and technical decisions. Treat `03-tech-decisions.md` as the source
  of truth for the intended Android architecture.
- `docs/` also contains interview evidence, problem framing, and the clickable
  prototype at `docs/prototype-artifact.html`.
- `design-bundle/` holds one HTML file per product screen plus `_base.css`.
- `design-system/we-climb/MASTER.md` defines visual tokens and Compose-facing
  design guidance.
- `shared/` contains KMP domain, media, and repository contracts reusable by
  future frontend and backend modules.
- `frontend/` is the Android Gradle root. `frontend/androidApp/` contains
  Android-specific CameraX, MediaStore, and sharing code.

Add a future backend as a sibling root-level module such as `backend/`. Do not
make backend code depend on `frontend/`; depend on `shared/` only when the
contract is genuinely cross-application.

## Build, Test, and Development Commands

Run commands from `frontend/`:

- `./gradlew :shared:jvmTest` runs shared Kotlin unit tests.
- `./gradlew :androidApp:assembleDebug` builds the Android debug APK.
- `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`
  installs the APK on a connected device.

You can inspect the current prototype by opening
`docs/prototype-artifact.html` in a browser. It is a design artifact, not a
replacement for device-level camera, trimming, storage, or sharing validation.

## Coding Style and Naming Conventions

Follow Kotlin and Jetpack Compose conventions once source code exists: use
four-space indentation, `PascalCase` for types and composables, and
`camelCase` for functions, properties, and packages. Prefer immutable data and
small feature-focused files. Validate inputs at application boundaries and
handle errors explicitly with user-facing messages where appropriate.

Use CameraX for capture, Media3 Transformer for trimming, and MediaStore for
successful video output as documented in `docs/planning/03-tech-decisions.md`.

## Testing Guidelines

The repository standard requires at least 80% coverage and unit, integration,
and end-to-end coverage. Place shared tests in `shared/src/commonTest/` and
Android-specific tests beside `frontend/androidApp/`. Name tests after behavior,
such as `recordsSuccessfulAttemptWhenVideoIsSaved`. Test camera and media
assumptions on a physical Android device before relying on emulator results.

## Delegation and Visible Workers

Classify work before delegating it. Use a visible cmux worker only when the
task can finish without the current conversation, tool results, or direct
coordination with an in-progress change. Typical examples are read-only
research, independent builds, and log collection. Start it with
`scripts/launch-visible-worker` and state its scope and completion criteria.

Use an internal subagent only when it needs the current task context or a
shared tool result. Keep final design decisions, integration, conflict
resolution, commits, and device verification with the primary agent.

Visible workers open as a new terminal split in the caller's current cmux
workspace and use the same repository. They run read-only by default. A worker
that changes code must use `--mode shared-write`; do not assign it files being
changed by another worker. Review its diff and rerun relevant verification.

## Active Work Gate

For a user request that authorizes implementation, create a local active-work
record before the first code change:

```sh
scripts/turn-gate start "Phase 1 Android implementation"
```

While this record exists, use commentary for progress updates and continue
working in the same turn. Do not send a final response merely because a small
implementation group, test, or skill checkpoint has completed.

Before a final response, run `scripts/turn-gate check-final`. A final response
is allowed only after one of these terminal conditions is true:

- the authorized work is complete and verification has run;
- a real blocker requires user input or an external state change;
- the user explicitly asks to stop or for a status-only response.

Clear the record only with the terminal reason:

```sh
scripts/turn-gate clear complete
scripts/turn-gate clear blocked
scripts/turn-gate clear status
```

This is a workflow guard, not a replacement for judgment. If the script and
the task state disagree, report the discrepancy and keep the task active.

### Continuous Implementation Override

When the current handoff or user request explicitly says to continue an
implementation without intermediate stops, that instruction takes precedence
over workflow skills that require approval checkpoints or human review between
implementation stages. Treat already approved design and scenario documents as
the implementation contract, keep reporting through commentary, and continue
tool calls until the authorized work is complete or a real external blocker
occurs.

Do not send a final response merely to report a successful partial build, a
fixed compiler error, or a workflow checkpoint. `scripts/turn-gate` records
the active task but cannot technically intercept a final response; the agent
must check its active status before ending a turn.

## Commit and Pull Request Guidelines

Use Conventional Commit-style messages seen in the history:
`feat(camera): add recording flow`, `docs(planning): update spike results`.
Keep subjects lowercase, imperative, under 50 characters, and without a
period. Pull requests should describe the user-facing change, link relevant
issues or planning documents, list verification performed, and include screen
captures for UI changes. Review the full diff before requesting review.
