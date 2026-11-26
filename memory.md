# HouseKeeper: Project memory

Decisions, conventions and quirks that aren't obvious from the code. **Read this and [`progress.md`](progress.md) before starting any new feature.** When a decision changes, update this file in the same PR and add a line to the decision log.

## Where things live

| What | File |
| --- | --- |
| Feature specification (what and why) | `HouseKeeper-Feature-Spec.md` |
| Feature tracker (what's built) | `progress.md` |
| Design tokens + components (single source of truth) | `design/hk.css` |
| Gas cylinder drawing + projection maths (port of the Compose Canvas) | `design/hk-cylinder.js` |
| Design system reference (every component, every state) | `design/design-system.html` |
| Clickable prototype (deep links: `prototype.html#gas`, `#reports`, …) | `design/prototype.html` |
| Earlier explorations (not current) | `design/variants.html`, `design/light-variants.html` |

The design pages open straight from disk in a browser. There is no build step.

## Design decisions

- **Direction: Amber Daylight.** Light theme. Its layout comes from exploration B ("Meter Display"), with the Home utility icon tiles taken from C.
- **Amber is a fill, not a text colour.** `#FFB020` is used for primary buttons, the Scan button, switches and selected states, always with `#241800` ink on top. Amber as text or icons uses `#8F5600`, because `#FFB020` on white is only 1.8:1.
- **Fonts:** Roboto for words. Roboto Mono for anything read off a meter: readings, units, rand, countdowns, join codes and uppercase labels.
- **Utility colours** are separate from brand and status colours: electricity `#B86E00`, water `#0A7C96`, gas `#C9402A`.
- **Daily usage is always a bar chart**, never a line graph. Grey bars are normal days, gas-red are anomalies, hatched are projected days, ink is the selected day, with a dashed average line.
- **Utility tiles are one component used twice.** On Home they open the scanner and show "last logged". On Reports they pick the utility, show the month-end value, and the selected tile gets a 2 dp amber border.
- **Scan button**: a raised amber button in the centre of the bottom nav on every tab. Four tabs: Home, Reports, Household, Settings.
- **The scanner shows** the OCR model in use, the utility switch, a Type option, and a digit-by-digit result where an unsure digit can be tapped to fix.
- **Activity is shown in a card** (avatar, action, mono value, when), never as loose text.
- **Gas cylinders always appear in their own colour and size proportions**, including list rows and the Home gas row. Never use a generic gas icon for a cylinder.
- **Report stats**: next 7 days with a confidence badge, month-end use, month-end cost, and what it's based on.
- **Every screen designs its empty, loading and error states.** An empty state says what's missing and why it matters, and gives one action.
- **Undo over confirm.** Saves show a snackbar with UNDO. Only irreversible actions open a confirmation dialog, and its button names the action.

## Gas cylinder model (keep in sync with `gas/model/GasCylinder.kt`)

- **Sizes** 4 / 9 / 14 / 48 kg, with default durations of 20 / 50 / 85 / 240 days (`CylinderSizeInfoList`). There is **no 19 kg size**.
- **Drawing heights**: 190 / 225 / 265 / 305 dp (`PropaneGasCylinderComposable`).
- **Colours** (`DefaultGasCylinderColors`): Propane Red `#D32F2F`, Industrial Blue `#1976D2`, Charcoal Slate `#37474F`, Camping Green `#2E7D32`, Safety Orange `#F57C00`, Steel Silver `#78909C`, Caution Yellow `#FBC02D`, Deep Teal `#00796B`, Plum Purple `#6A1B9A`.
- **Projection (new, from the prototype):**
  - Rate: kg/day = the average of `size ÷ days lasted` over finished cylinders.
  - Level = (size − rate × days in use) ÷ size.
  - Days left = kg left ÷ rate.
  - Fallback with no history: size ÷ the default duration.
  - Days-left colour: under 5 days Danger, under 10 Electricity amber, otherwise Success.

## Repo and Git conventions

- **GitFlow** (use the `/gitflow` skill): branch from `develop` as `feature/` or `chore/`; open a PR into `develop`. Never commit straight to `main` or `develop`.
- **`origin`** is the fork `ratjatjimalatji/HouseKeeper---Android-Utility-App`. **`upstream`** is `YanneckReiss/JetpackComposeMLKitTutorial`.
- **`gh` defaults to the upstream repo.** Always pass `--repo ratjatjimalatji/HouseKeeper---Android-Utility-App`, or the PR or issue goes to the wrong project.
- **Issues are turned off on the fork.** Branches have no issue number (`chore/design-docs-progress-tracking`), and the PR description carries the problem statement and acceptance criteria. If issues get turned on, go back to `type/<issue>-<slug>`.
- **Conventional Commits** with a body explaining what and why.
- **Merge PRs with a merge commit** (not squash) when a branch carries several features, so each commit stays traceable.
- **Every feature PR updates `progress.md`.**

## App facts

- Kotlin, Jetpack Compose, Material 3, MVVM with `StateFlow`, Hilt, Room, CameraX, ML Kit. `minSdk 28`, `targetSdk 34`.
- **Package** `de.yanneckreiss.mlkittutorial`, **applicationId** `de.yanneckreiss.cameraxtutorial`. Both must be renamed before the first Play Store upload (progress 9.5).
- The app is still branded "House Check". The final name is an open decision (spec §9 Q8).
- Backend features (accounts, households, sync, push, AI suggestions, supplier orders) are blocked on spec §9 Q1–Q5. Build the UI against fake repositories behind interfaces so it can be wired up later.

## Decision log

| Date | Decision |
| --- | --- |
| 2026-11-26 | Chose Amber Daylight (layout B + C's tiles) out of three explorations and three light themes. |
| 2026-11-26 | Daily usage uses bar charts. Report utility picker reuses the Home tiles. Activity goes in a card. |
| 2026-11-26 | Gas tracker keeps the existing behaviour (carousel, row pickers, dynamic sizing) and adds a projected level, history-based kg/day, and real-colour mini cylinders. |
| 2026-11-26 | Merged `docs/issue-7-readme-showcase-screenshots` into `develop` (PR #10) as the baseline before the redesign. |
