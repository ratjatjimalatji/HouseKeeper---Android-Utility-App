# HouseKeeper: Progress

Tracks every feature in the approved prototype (`design/prototype.html`) against the Android app.

**Before starting any feature:**
1. Read [`memory.md`](memory.md) for the decisions and conventions.
2. Find the feature here. Open its prototype screen (`design/prototype.html#<key>`) and the matching section of [`design/design-system.html`](design/design-system.html).
3. Branch with GitFlow from `develop`.
4. Update this file **in the same PR**: tick what's done and add the PR number.

**Status legend**

| Mark | Meaning |
| --- | --- |
| ✅ | Done: built in Compose, matches the design, merged to `develop` |
| 🟡 | Partly there: logic exists in the app, but not the new design or behaviour |
| ⬜ | Not started |
| ⛔ | Blocked: needs backend or a decision (see spec §9) |

Prototype keys open a screen directly, for example `design/prototype.html#gas`.

---

## 0. Foundations (do these first)

| # | Item | Design ref | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 0.1 | Amber Daylight colour tokens as `HkColors` (all hex values in `hk.css`) | DS › Colour | `ui/theme/Color.kt`, `Theme.kt` | ⬜ | |
| 0.2 | Type scale `HkType`: Roboto + Roboto Mono (readings, money, labels) | DS › Typography | `ui/theme/Type.kt`, add Roboto Mono font | ⬜ | |
| 0.3 | Shape, spacing and elevation tokens | DS › Spacing | `ui/theme/` | ⬜ | |
| 0.4 | `HkButton` (primary, secondary, tonal, ghost, danger; default, pressed, focused, disabled, loading) | DS › Buttons | `ui/components/` | ⬜ | |
| 0.5 | Inputs: text field and reading field with unit, help, warning and error | DS › Inputs | `ui/components/` | ⬜ | |
| 0.6 | Chips, segmented buttons, switches, radios | DS › Chips, Switches | `ui/components/` | ⬜ | |
| 0.7 | Badges (confidence, sync, severity), role tags, avatars | DS › Badges | `ui/components/` | ⬜ | |
| 0.8 | Card, list row, `UtilityTile` (Home and Reports variants, selected state) | DS › Cards, Tiles | `MeterNavigationCard.kt` → `UtilityTile` | 🟡 | |
| 0.9 | Skeletons, linear/indeterminate progress, spinner | DS › Loading | `ui/components/` | ⬜ | |
| 0.10 | Snackbar, banners (offline, sync), alert card | DS › Snackbars | existing snackbar + new banners | 🟡 | |
| 0.11 | Bottom sheet and destructive dialog | DS › Sheets | `ui/components/` | ⬜ | |
| 0.12 | `HkEmpty` empty-state component | DS › Empty states | `ui/components/` | ⬜ | |
| 0.13 | Bottom nav (4 tabs + raised amber Scan button) and app bar with household switcher | DS › Navigation | `AppNavHost.kt`, `ui/MainScreen.kt` | ⬜ | |
| 0.14 | Bar chart for daily usage (normal, anomaly, projected, selected, average line) | DS › Charts | replaces `ReportsLineGraph.kt` | ⬜ | |
| 0.15 | Stat grid (value + change or confidence) | DS › Charts | `ReportsProjectionCards.kt` | 🟡 | |

## 1. Home (`#home`)

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 1.1 | Utility tiles (Electricity, Water, Gas) → scanner / gas tracker, with "last logged" | 4.3 | `MeterSelectionScreen.kt` | 🟡 | |
| 1.2 | Electricity run-out hero: countdown, urgency colour, gauge, Buy / Top-up | 4.4, 4.6 | new `RunOutCard` | ⬜ | |
| 1.3 | Gas row: real cylinder (colour, size, level), days left, order-by date | 4.8 | uses `PropaneGasCylinderComposable` (mini) | ⬜ | |
| 1.4 | Water row and month cost row | 4.4, 4.5 | new | ⬜ | |
| 1.5 | Recent activity **card** (avatar, action, value, when; last 3; opens feed) | 4.2 | new `ActivityCard` | ⬜ | |
| 1.6 | Household switcher in the app bar (sheet) | 4.2 | new | ⛔ backend | |
| 1.7 | Offline / syncing banner | 4.3, §6 | new | ⛔ backend | |

## 2. Log (`#scanE`, `#scanW`, `#manual`, `#topup`, `#history`)

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 2.1 | Camera scan, electricity + water | 4.3 | `ui/camera/CameraScreen.kt` | 🟡 restyle | |
| 2.2 | Top bar shows the OCR model in use (tap to cycle) + torch | 4.3, 4.9 | `CameraScreen.kt`, `ocr/OcrModelManager.kt` | 🟡 | |
| 2.3 | Utility switch (Electricity / Water) on the scanner | 4.3 | `CameraScreen.kt` | 🟡 | |
| 2.4 | Digit-by-digit result; unsure digit highlighted; **tap a digit to fix it** | 4.3 | new `ReadingDigits` | ⬜ | |
| 2.5 | Sense check on the scanner ("+11.2 kWh · normal" / lower than last) | 4.3 | `MeterReadingParser.kt` + new | ⬜ | |
| 2.6 | Scanner states: scanning, reading found, nothing found (torch, type) | 4.3 | `CameraScreen.kt` | ⬜ | |
| 2.7 | "Type the reading" prefilled from the scan; live sense checks (lower, far above normal, invalid) | 4.3 | `view/EnterElectricityReading.kt` | 🟡 | |
| 2.8 | Save → snackbar with UNDO | 4.3 | existing | ✅ (PR #3, restyle with 0.10) | |
| 2.9 | Log a top-up (amount, units, preview of new balance) | 4.3 | new screen | ⬜ | |
| 2.10 | Reading history: utility switch, sync badges, delete with confirmation dialog | 4.3 | `view/EntriesHistoryScreen.kt` | 🟡 | |
| 2.11 | Optional meter photo ("Keep a photo") | 4.3 | new | ⛔ S3 | |

## 3. Usage (`#usageE`, `#usageW`)

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 3.1 | Electricity detail: run-out hero + daily bars + latest readings | 4.4 | new `UsageScreen` | ⬜ | |
| 3.2 | Water detail: per-day and month stats instead of run-out | 4.4 | new `UsageScreen` | ⬜ | |
| 3.3 | Tap a bar to inspect a day | 4.4 | 0.14 | ⬜ | |
| 3.4 | Daily usage spread between readings (count up or down) | 4.4 | `TrendsProjectionEngine.kt` | ✅ | |

## 4. Reports (`#reports`, `#period`, `#export`)

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 4.1 | **Reuse the Home utility tiles** as the utility picker (month-end value, selected border) | 4.5 | replaces `UtilityToggleRow.kt` | ⬜ | |
| 4.2 | Month chips + Custom chip | 4.5 | `MonthSelectorRow.kt` | 🟡 | |
| 4.3 | Stat grid: next 7 days **with confidence**, month-end use, month-end cost, based on N readings | 4.4 | `ReportsProjectionCards.kt` | 🟡 | |
| 4.4 | Daily usage as a **bar chart** (not line) with anomaly explanation | 4.4 | replaces `ReportsLineGraph.kt` | ⬜ | |
| 4.5 | Tariff and cost in rand | 4.4 | `ReportsViewModel.kt` | ⬜ | |
| 4.6 | Custom period sheet (presets + pick dates → usage, cost, confidence) | 4.4 | new sheet | ⬜ | |
| 4.7 | Export sheet: PDF or CSV, share or copy link | 4.5 | `export/UtilityExportManager.kt` (CSV exists) | 🟡 | |
| 4.8 | AI suggestion card with Helpful / Not relevant / Already doing this | 4.7 | new | ⛔ backend | |
| 4.9 | Weather context on anomalies | 4.4 | `WeatherCorrelationEngine.kt` | 🟡 | |
| 4.10 | Anomaly detection | 4.4 | `AnomalyDetector.kt` | ✅ | |

## 5. Share / Household (`#household`, `#share`, `#join`, `#activity`)

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 5.1 | Join code card with QR, Share, New code (confirm dialog) | 4.2 | new `HouseholdScreen` | ⛔ backend | |
| 5.2 | Share sheet (WhatsApp, SMS, Copy, More) | 4.2 | new | ⛔ backend | |
| 5.3 | Join request card: approve / decline with UNDO | 4.2 | new | ⛔ backend | |
| 5.4 | Members list with role tags; member sheet to change role or remove | 4.2 | new | ⛔ backend | |
| 5.5 | Join with a code: validation, checking, waiting for approval | 4.2 | new | ⛔ backend | |
| 5.6 | Activity feed screen (grouped by day) | 4.2 | new | ⛔ backend | |

## 6. Gas (`#gas`, `#order`)

Rework of the existing `GasCylinderTrackerScreen`. Keep its behaviour and change the presentation.

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 6.1 | Carousel: page 1 = dashed new cylinder; opens on the **active** cylinder; arrows + dots | 4.8 | `GasCylinderTrackerScreen.kt` (`CylinderHeroVisualCard`) | 🟡 | |
| 6.2 | Cylinder drawing in its own colour, **height by size** (4/9/14/48 → 190/225/265/305) | 4.8 | `PropaneGasCylinderComposable.kt` | ✅ (restyle decal) | |
| 6.3 | **Projected gas level** on the drawing (faded empty part + dashed line) | 4.8 | `PropaneGasCylinderComposable.kt` add `level` | ⬜ | |
| 6.4 | Side panel: days left (urgency colour), % and kg left, empty date | 4.8 | `GasCylinderTrackerScreen.kt` | ⬜ | |
| 6.5 | 14-day "gas left" projection bars (today in cylinder colour) | 4.8 | new | ⬜ | |
| 6.6 | Projection from **past cylinders' kg/day** (average of size ÷ days lasted; default from `CylinderSizeInfoList`) | 4.8 | `GasCylinder.kt` / `GasCylinderViewModel.kt` | ⬜ | |
| 6.7 | Details card: Status segmented (Active / Used up) | 4.8 | `DynamicColorPaletteSection` | 🟡 restyle | |
| 6.8 | Size options with mini cylinders at real proportions in the chosen colour | 4.8 | `DynamicColorPaletteSection` | 🟡 restyle | |
| 6.9 | Colour swatch row (9 colours, Text-coloured selection ring) + tap cylinder to cycle | 4.8 | `DynamicColorPaletteSection` | 🟡 restyle | |
| 6.10 | Collapsible service dates & details (bought, start, supplier, notes, in service / stored) | 4.8 | existing card | 🟡 restyle | |
| 6.11 | Save / Update with loading + UNDO snackbar | 4.8 | existing | 🟡 restyle | |
| 6.12 | History rows: **mini cylinder in its real colour and size** (not a generic icon), kg/day, active level, delete with UNDO | 4.8 | `CylinderHistoryItemCard` | 🟡 | |
| 6.13 | Swap cylinder → new page; saving makes the old one finished | 4.8 | `GasCylinderViewModel.kt` | 🟡 | |
| 6.14 | Order gas: size chips 4/9/14/48, supplier list (sponsored label), prices | 4.8 | new `OrderGasScreen` | ⛔ supplier data | |
| 6.15 | Supplier sheet → send on WhatsApp → "Order sent" card → Mark delivered = new active cylinder | 4.8 | new | ⛔ supplier data | |

## 7. Notifications & Settings (`#notifications`, `#notifsettings`, `#settings`)

| # | Feature | Spec | Compose target | Status | PR |
| --- | --- | --- | --- | --- | --- |
| 7.1 | Notification centre (each item opens the right screen) | 4.6 | new | ⬜ | |
| 7.2 | Notification settings: switches + quiet hours | 4.6 | new | ⬜ | |
| 7.3 | Anomaly notifications (local worker) | 4.6 | `worker/UtilityAnomalyWorker.kt` | ✅ (moves server-side later) | |
| 7.4 | Settings: profile, app, scanner model radios with download progress, privacy, delete account dialog | 4.9 | `settings/ui/SettingsScreen.kt` | 🟡 | |
| 7.5 | OCR model download + selection | 4.9 | `ocr/OcrModelManager.kt` | ✅ (PR #4) | |

## 8. Empty, loading and offline states

Each screen above is only done when these states are built too.

| # | State | Prototype key | Status |
| --- | --- | --- | --- |
| 8.1 | New household · Home ("Log your first reading") | `#eHome` | ⬜ |
| 8.2 | Reports · not enough data (stat grid shows "—") | `#eReports` | ⬜ |
| 8.3 | Usage · needs 2 more readings | `#eUsage` | ⬜ |
| 8.4 | Household · only you | `#eHouse` | ⬜ |
| 8.5 | History · no readings | `#eHist` | ⬜ |
| 8.6 | Notifications · all caught up | `#eNotif` | ⬜ |
| 8.7 | Gas · no cylinder (banner + new cylinder page) | `#eGas` | ⬜ |
| 8.8 | Order · no suppliers near you | `#eOrder` | ⬜ |
| 8.9 | Scanner · can't find a reading | `#eScan` | ⬜ |
| 8.10 | Loading skeletons (Home, Reports, Household) | Data → Loading | ⬜ |
| 8.11 | Offline save → "waiting to sync" → synced | Network → Offline | ⛔ backend |

## 9. Platform & Play Store (from the spec, not in the prototype)

| # | Item | Spec | Status |
| --- | --- | --- | --- |
| 9.1 | Accounts, onboarding, create/join household | 4.1 | ⛔ spec §9 Q1–2 |
| 9.2 | API on EC2, database, S3 pre-signed URLs | §6 | ⛔ spec §9 Q1 |
| 9.3 | Room as offline cache + outbox sync | §6 | ⬜ |
| 9.4 | FCM push, server-side reminders | 4.6 | ⛔ backend |
| 9.5 | Rename package/applicationId off `de.yanneckreiss.*` | §7 | ⬜ (before first upload) |
| 9.6 | One app name (HouseKeeper vs House Check) | §7, §9 Q8 | ⛔ decision |
| 9.7 | Sample data only in debug builds | §7 | ⬜ |
| 9.8 | Privacy policy, data safety form, account deletion link, ads declaration | §7 | ⬜ |

---

## Change log

| Date | Change | PR |
| --- | --- | --- |
| 2026-11-26 | Created from the approved prototype (Amber Daylight). Baseline status taken from `develop` after PR #10. | this PR |
