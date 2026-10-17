# Contributing to Cufflink

Thanks for helping! A few house rules:

- `./gradlew assembleDebug testDebugUnitTest lintDebug` must stay green.
- Anything both phone and watch must agree on (wire format, names, kinds) lives in
  `:shared`, with a unit test. Never copy it into one app.
- Changing the beam header? Bump `BeamProtocol.VERSION`, since old and new builds must refuse
  each other instead of misreading each other.
- Watch UI: Compose for Wear OS Material 3. Lists use `CuffList`, and anything the bezel can
  drive uses `Modifier.rotary` + `NotchCounter`.
- In PRs, say what you tested on real hardware and what you didn't.
