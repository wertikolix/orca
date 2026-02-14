## Summary

Describe the change and why it is needed.

## Type of Change

- [ ] Bug fix
- [ ] Feature
- [ ] Refactor
- [ ] Docs
- [ ] Build/CI

## Verification

List commands and results.

```bash
./gradlew --no-daemon --build-cache :orca-core:test :orca-compose-android:testDebugUnitTest :sample-app:assembleDebug
```

## Checklist

- [ ] Tests added/updated where needed
- [ ] README/release notes updated if behavior changed
- [ ] No unrelated edits
- [ ] Security impact considered
