## What changed

<!-- Which modules: api / loader / fabric / neoforge / examples / docs? -->

## Why

<!-- Link the issue or explain the motivation -->

## Verification

- [ ] Full build passes and the suite prints `ALL TESTS PASSED (8)`
- [ ] Adapter changes were exercised on a real Fabric 26.3 and/or NeoForge 26.3.0.115 server (describe how)
- [ ] For both-adapter changes, the Gradle-built jars were exercised twice per platform as described in `docs/VERIFICATION.md`
- [ ] Signal/block-entity changes were exercised with a fake-player harness where applicable
- [ ] Duplicate-id and dependency-error paths still behave defensively
- [ ] Public API changes updated `docs/API.md` and `CHANGELOG.md`
- [ ] No `.cache/`, `build/`, `run/`, server jar or game files are included
- [ ] `dist/` jars were not touched by hand (maintainers rebuild them at release)

## Notes

<!-- Migration notes, known limitations, follow-up work -->
