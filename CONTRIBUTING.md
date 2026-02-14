# Contributing to Orca

Thanks for contributing.

## Before You Start

- Open an issue for non-trivial changes.
- Keep PRs focused and small.
- Follow existing naming and architecture style.

## Local Setup

Requirements:

- JDK 17
- Android SDK (`platforms;android-36`, `build-tools;36.0.0`)

Run verification locally:

```bash
./gradlew --no-daemon --build-cache :orca-core:test :orca-compose-android:testDebugUnitTest :sample-app:assembleDebug
```

Optional release-like check:

```bash
./gradlew --no-daemon --build-cache :sample-app:assembleRelease :sample-app:bundleRelease
```

## Commit and PR Guidelines

- Use clear commit messages.
- Add or update tests for behavior changes.
- Update docs when public behavior changes.
- Keep API changes intentional and documented.

PR checklist:

- [ ] Tests pass locally
- [ ] Docs/release notes updated if needed
- [ ] No unrelated changes included
- [ ] Security-sensitive behavior reviewed

## Reporting Bugs

Use the bug report issue template and include:

- Orca version
- Android/Compose versions
- Minimal markdown input that reproduces the issue
- Expected vs actual result

## Security

Please do not open public issues for security vulnerabilities.
See `SECURITY.md` for the disclosure process.
