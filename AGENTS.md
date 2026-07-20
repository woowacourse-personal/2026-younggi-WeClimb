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

## Commit and Pull Request Guidelines

Use Conventional Commit-style messages seen in the history:
`feat(camera): add recording flow`, `docs(planning): update spike results`.
Keep subjects lowercase, imperative, under 50 characters, and without a
period. Pull requests should describe the user-facing change, link relevant
issues or planning documents, list verification performed, and include screen
captures for UI changes. Review the full diff before requesting review.
