# Fallout Terminal Hacker

Android app that uses your phone camera to read Fallout terminal screens and recommend optimal guesses for the hacking minigame.

## Flow

1. Point camera at the terminal screen, tap **SCAN TERMINAL**.
2. ML Kit OCR runs on-device — no network, no API key.
3. `WordExtractor` filters out the symbol garbage and keeps the candidate words.
4. `Solver` runs minimax over likeness partitions and recommends a word.
5. Tap a candidate (the recommended one is pre-selected), type the likeness the game reports, hit **SUBMIT GUESS**.
6. Repeat until **ACCESS GRANTED** or **LOCKOUT**.

You can tap **DEL** next to any candidate if OCR picked up a word that isn't really in the puzzle.

## Stack

- Kotlin 2.0.20, Jetpack Compose, Material 3
- CameraX 1.3.4
- ML Kit Text Recognition 16.0.1 (fully on-device)
- minSdk 26 (Android 8.0), targetSdk 34

## Build

Open in Android Studio (Jellyfish or newer): **File → Open** the project root. It will sync Gradle and generate the wrapper jar automatically. Then **Run** to a connected device.

Or from CLI if you have a system Gradle installed:

```bash
gradle wrapper        # generates gradle-wrapper.jar
./gradlew test        # run the JVM unit tests
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/kotlin/com/n3network/falloutterminalhacker/
├── MainActivity.kt
├── camera/CameraView.kt        # CameraX preview as a Composable
├── ocr/
│   ├── TextRecognizer.kt       # ML Kit wrapper, suspend API
│   └── WordExtractor.kt        # OCR text → candidate word list
├── solver/
│   ├── Solver.kt               # likeness + minimax
│   └── SolverState.kt          # immutable 4-attempt game state
└── ui/
    ├── HackerApp.kt            # screen routing
    ├── CameraScreen.kt
    ├── SolverScreen.kt
    └── theme/                  # Pip-Boy green-on-black
```

## The solver, in plain English

After each guess, the game tells you the **likeness**: how many letters of your guess are in the same position as the password. So if the password definitely has likeness `k` with your guess, you can throw away every candidate that doesn't.

To pick the next guess, partition the candidate list by what likeness each word would produce against a hypothetical guess `g`. The biggest partition is the worst case — the most candidates you could be stuck with after observing the result. Pick the `g` that makes that worst case smallest. That's `Solver.bestGuess`, about 10 lines of Kotlin.

## Tests

```bash
./gradlew test
```

Covers `Solver` (likeness math, filtering, minimax determinism, state transitions including solve/fail) and `WordExtractor` (length-dominant filtering, dedup, normalization).

## Known limitations / next steps

- OCR struggles with screen glare and angle distortion. Hold the phone roughly parallel to the screen for best results.
- If OCR misses a word, the solver may not converge on the actual password. The **DEL** button lets you remove false positives; a future addition would be a manual **+ ADD** to type missing words in.
- The bracket-pair mechanics (`<>`, `[]`, `()`, `{}` for dud-remove / allowance-restore) aren't detected yet.
- No preprocessing — isolating the green channel before OCR would likely improve recognition on CRT scanlines.
