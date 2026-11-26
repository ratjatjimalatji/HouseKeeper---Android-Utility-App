# 🏠 HouseKeeper: Feature Specification

> 💡 **Summary:** HouseKeeper is moving from a private, on-device Android app to an online-first app that families share. People log electricity, water and gas readings in seconds, see where usage is heading, get reminded before they run out, and can order gas or buy electricity right when they need it.

| Property | Value |
| --- | --- |
| **Product** | HouseKeeper (currently branded "House Check" in the app) |
| **Platform** | Android (Jetpack Compose, minSdk 28) |
| **Backend** | Amazon EC2 (API) + Amazon S3 (files) |
| **Status** | Draft v1.0 |
| **Owner** | Ratjatji Malatji |
| **Last updated** | 26 November 2026 |
| **Based on** | Branch `docs/issue-7-readme-showcase-screenshots` |
| **Target release** | Google Play Store: closed testing, then production |

---

## Contents

1. [Project overview](#1-project-overview)
2. [Goals and principles](#2-goals-and-principles)
3. [Where the app is today](#3-where-the-app-is-today)
4. [Features](#4-features)
5. [Screens](#5-screens)
6. [Platform and architecture](#6-platform-and-architecture)
7. [Play Store readiness](#7-play-store-readiness)
8. [Out of scope](#8-out-of-scope)
9. [Open questions](#9-open-questions)

---

## 1. Project overview

### The problem

A utility bill tells a household what it spent last month. By the time the bill arrives it is too late to change anything. Prepaid electricity runs out without warning, often at night. A gas cylinder goes empty halfway through cooking. Nobody in the house has the full picture, because whoever bought the electricity or swapped the cylinder is the only one who knows.

### The product

HouseKeeper turns a few seconds of meter logging into a live view of a household's utilities:

- **Log** a reading by pointing the camera at the meter, or by typing it.
- **See** daily usage, weekly and monthly projections, and trends.
- **Know in advance** when electricity units or a gas cylinder will run out.
- **Act** by ordering gas or getting a reminder to buy electricity before it's needed.
- **Share** all of this with the people you live with, or the family members you look after, through a shared group joined with a code (similar to Life360 circles).

### Who it's for

| User | What they need |
| --- | --- |
| **Household manager** | The person who buys electricity and gas. Wants to know when to buy, and how much it will cost this month. |
| **Household member** | Lives in the house, logs a reading now and then. Wants it to take seconds. |
| **Remote family member** | Supports parents or relatives in another home. Wants to see that their utilities are healthy and get alerted when something runs low. |
| **Landlord or caretaker (later)** | Looks after a few properties. Wants every property's readings in one place. |

### What changes in this version

| Today (private build) | Next (shareable, Play Store) |
| --- | --- |
| Data lives only on one phone (Room database) | Data lives in the cloud and syncs to every member's phone |
| One user, no account | Accounts, profiles and household groups with join codes |
| Sample data seeded on first launch | Real onboarding: create or join a household |
| Reports and projections | Reports, projections, AI suggestions and predicted run-out dates |
| Anomaly notifications (local worker) | Reminders and alerts sent to every group member |
| CSV export to the share sheet | PDF and CSV reports, stored in S3, shareable by link |
| No monetisation | Gas ordering through partner suppliers and adverts |

---

## 2. Goals and principles

### Goals

1. **Logging takes under 10 seconds.** From opening the app to a saved reading, using the camera or the keypad.
2. **No surprise run-outs.** Every household with enough readings gets a warning at least 2 days before prepaid electricity or a gas cylinder runs out.
3. **Families share one picture.** Any member can log; every member sees the same numbers within seconds.
4. **Ready to share publicly.** The app passes Google Play review and is something friends and family can install from the store, not from an APK.
5. **Pays for its own hosting.** Gas ordering commissions and adverts cover the EC2 and S3 costs.

### Success measures

| Measure | Target for the first 3 months after launch |
| --- | --- |
| Median time to log a reading | Under 10 seconds |
| Households logging at least twice a week | 50% of active households |
| Households with 2 or more members | 40% |
| Run-out warnings sent before the run-out happens | 90% |
| Crash-free sessions | 99.5% |

### Principles

- **Logging comes first.** No feature is allowed to slow down or clutter the logging flow. Adverts never appear on the scanner or entry screens.
- **Readings, not guesses.** Every projection is built from real readings and says how confident it is. When there isn't enough data, the app says so instead of showing a made-up number.
- **Online first, never blocked.** The cloud is the source of truth, but a reading taken in a cupboard with no signal is saved on the phone and synced when the connection returns.
- **Share on purpose.** A member only sees a household's data after they've joined with a code and been accepted. Nothing is shared by default.
- **Explain every number.** Projections, suggestions and alerts show the reason behind them ("based on 12.5 kWh/day over the last 7 days").
- **Undo over confirm.** Quick actions save straight away with an Undo option, so the common path has no extra taps. Only destructive actions that can't be undone ask for confirmation.
- **Ads earn their place.** Adverts and sponsored suppliers must be relevant to running a home, clearly labelled, and easy to ignore.

---

## 3. Where the app is today

The current branch already has a strong logging and reporting core. This spec builds on it rather than replacing it.

| Area | What exists | Reused in this spec |
| --- | --- | --- |
| Camera scanning | CameraX + ML Kit text recognition for electricity (kWh) and water (m³ / L) meters, with a parser for spaced unit labels | Quick logging |
| Manual entry | "Can't scan? Type the reading" with number validation | Quick logging |
| Undo | Snackbar with UNDO after every save | Quick logging |
| Reports | Weekly and monthly projections, interactive trend graph, month selector, utility toggle | Usage trends, Reports |
| Projection engine | Daily usage spread between consecutive readings; works for meters counting up or down | Predictions |
| Gas cylinder tracker | Swipeable cylinders (4–48 kg), status, colour, days in use, estimated days left | Gas tracking and ordering |
| Anomaly detection | Spike detection with severity levels, background worker that sends notifications | Alerts |
| Weather correlation | Weather score per reading and a breakdown of weather's impact | AI suggestions |
| Export | CSV export through the Android share sheet | Exporting reports |
| OCR settings | Choice of ML Kit or downloadable models for slashed-zero and 7-segment displays | Settings |

---

## 4. Features

The app has nine feature areas. The core of the product is **quick logging, usage trends, reports and exporting** (areas 3 to 5); everything else supports or builds on them. Each item says what the feature does and why it's there.

### 1. Onboarding and accounts

- **Welcome.** Three short screens explain what HouseKeeper does: log, predict, share. Users can skip them. **Why:** people decide in the first minute whether an app is worth keeping, and most of them won't read more than a sentence per screen.
- **Sign up and sign in.** Users create an account with Google sign-in or email and password. **Why:** data now lives in the cloud and is shared between people, so every reading needs an owner.
- **Create a profile.** Users add a display name and an optional photo. **Why:** in a shared household, other members need to see who logged a reading or bought electricity.
- **Create or join a household.** After the profile, users choose "Create a household" or "Join with a code". **Why:** a household is the unit everything else hangs off; nobody should land on an empty home screen without one.
- **Set up meters.** When creating a household, users pick which utilities they track (prepaid or postpaid electricity, water, natural gas, LPG cylinders) and enter a first reading for each. **Why:** projections need a starting point, and knowing whether electricity is prepaid changes which reminders make sense.
- **Permissions on demand.** Camera and notification permissions are requested at the moment they're first needed, with a line explaining why. **Why:** asking for everything up front makes people say no.
- **Delete account.** Users can delete their account and data from inside the app and from a web link. **Why:** Google Play requires both, and it's the right thing to offer.

### 2. Households and family groups

- **Household join code.** Every household has a short code (for example `HK-4F7Q2`) that the owner can share by WhatsApp, SMS or a QR code. **Why:** joining should be as easy as sending a message; this is the same pattern people already know from Life360.
- **Join requests.** Entering a code sends a request that the owner or an admin approves. Codes can be regenerated, which stops old codes working. **Why:** a code can be forwarded further than intended; approval keeps a household's data private.
- **Roles.** Owners can do everything; admins can approve members and edit meters; members can log readings and view reports; viewers can only see. **Why:** a remote family member may only need to watch, while the people in the house need to log.
- **Multiple households.** One account can belong to several households and switch between them from the home screen. **Why:** someone may track their own home and their parents' home.
- **Activity feed.** A household timeline shows who logged what and when, who bought electricity, and who swapped a cylinder. **Why:** it answers "did anyone buy electricity?" without a phone call.
- **Leave or remove.** Members can leave a household; owners and admins can remove members. **Why:** people move out, and access should end when they do.

### 3. Quick logging

- **One-tap logging from home.** Each tracked utility has a card on the home screen that opens straight to the scanner for that meter. **Why:** logging is the action people repeat most, so it must be the shortest path in the app.
- **Camera scan.** The camera reads the meter display in real time and fills in the reading, using the existing ML Kit scanner and the optional slashed-zero and 7-segment models. **Why:** typing long meter numbers is slow and error-prone.
- **Type the reading.** "Can't scan? Type the reading" opens a number field pre-filled with whatever the camera detected. **Why:** glare, dirty dials and dark cupboards defeat any scanner.
- **Sense checks.** Before saving, the app flags readings that look wrong: lower than the last reading on a meter that counts up, or far outside the normal daily range. Users can save anyway. **Why:** one mistyped digit ruins a month of projections.
- **Undo.** Every save shows a snackbar with UNDO. **Why:** saving without a confirmation dialog keeps logging fast, and undo covers the mistakes.
- **Log a top-up or a cylinder swap.** Users can record buying electricity (amount paid, units received) and swapping a gas cylinder (size, supplier, price) as events alongside readings. **Why:** projections need to know when units were added; otherwise a top-up looks like negative usage.
- **Offline capture.** Readings taken without a signal are saved on the phone, marked "waiting to sync", and uploaded automatically later. **Why:** meters are often in places with poor reception.
- **Optional photo.** Users can keep the photo of the meter with the reading; it is stored in S3. **Why:** a dated photo is proof in a billing dispute with the municipality or landlord.
- **Edit and delete readings.** Readings can be edited or deleted from history; deleting asks for confirmation and records who did it in the activity feed. **Why:** deletion can't be undone, and other members should know when shared data changes.

### 4. Usage trends and predictions

- **Daily usage.** Usage is worked out from the difference between consecutive readings, spread evenly across the days between them, for meters that count up or down. **Why:** this is the method the current projection engine uses, and it works with irregular logging.
- **Weekly and monthly projections.** The app shows units expected for the coming week and a month-end estimate, with the change against the previous period. **Why:** people budget by week and month.
- **Custom period prediction.** Users can pick any date range (for example, until the next payday or until a holiday) and see expected usage and cost for it. **Why:** real budgets don't always follow calendar months.
- **Run-out prediction.** For prepaid electricity and gas cylinders, the app shows the estimated date units or gas will run out. **Why:** running out is the problem people most want to avoid.
- **Cost estimates.** Users can set a tariff per unit (or a price per cylinder size) and see usage in rand as well as units. **Why:** money is easier to act on than kWh.
- **Confidence indicator.** Every projection shows how reliable it is (for example, "Low confidence: only 2 readings this month"). **Why:** an estimate built on two readings shouldn't look as certain as one built on twenty.
- **Interactive trend graph.** Solid points for logged readings, a dotted line for usage between them; drag to inspect any day. **Why:** it's already built and it's the clearest way to see a pattern.
- **Anomaly detection.** The app spots usage spikes (for example, a leaking toilet or a geyser left on) and highlights them on the graph and in alerts. **Why:** catching waste in days instead of at the end of the month saves real money.
- **Weather context.** Each reading carries a weather score, and trends show how temperature and rain affect usage. **Why:** it explains why winter months look worse and stops people worrying about normal seasonal changes.

### 5. Reports and exporting

- **Quick report.** One tap on the home screen gives a summary card for the current month: units used, cost so far, month-end estimate, run-out dates and the biggest change since last month. **Why:** most people want the answer, not the full report.
- **Full report.** For any utility and month: projections, trend graph, reading history and anomalies. **Why:** this is the existing Reports screen, extended to cloud data and cost.
- **Household comparison.** Users who belong to more than one household can compare them side by side. **Why:** someone looking after their parents' home wants to know if usage there is normal.
- **Export to PDF.** Reports can be exported as a formatted PDF with the household name, period, totals, graph and reading list. **Why:** a PDF is what people send to a landlord, municipality or family member.
- **Export to CSV.** All readings for a chosen period can be exported as CSV, as today. **Why:** some users want to work with the numbers in a spreadsheet.
- **Share by link.** Exports are stored in S3 and can be shared as a time-limited link as well as through the share sheet. **Why:** a link is easier to send in a WhatsApp group than a file, and it expires so data doesn't stay public forever.
- **Monthly summary.** At the start of each month, every member gets a notification with last month's summary. **Why:** it brings people back to the app at the moment the information is most useful.

### 6. Reminders and alerts

- **Buy electricity reminder.** When prepaid electricity is predicted to run out within a set number of days (default 3), every member who opted in gets a reminder, repeated once a day until someone logs a top-up. **Why:** this is the most-requested feature and the most painful problem to prevent.
- **Gas running low.** When a cylinder is predicted to run out within a set number of days, members get a reminder with a shortcut to order gas. **Why:** swapping a cylinder takes planning; finding out it's empty mid-meal is too late.
- **Log a reading reminder.** If no reading has been logged for a set number of days, the app nudges the household. **Why:** projections are only as good as the readings behind them.
- **Unusual usage alert.** Anomalies of warning or critical severity are sent to all members, with the reason. **Why:** a burst pipe affects everyone in the house, not just the person who logged.
- **Notification settings.** Each member chooses which alerts they get, per household, with quiet hours. **Why:** a remote viewer may only want run-out alerts, and nobody wants a 2 a.m. notification about a water meter.
- **Server-side scheduling.** Reminders are calculated and sent by the backend through push notifications, not by a worker on one phone. **Why:** in a shared household the alert must reach everyone, even if the person who logged has the app closed.

### 7. AI suggestions

- **Personalised suggestions.** The app generates short, specific tips from the household's own data (for example, "Electricity use on weekdays between 6 and 8 a.m. is 40% higher than weekends. A geyser timer could save around R180 a month"). **Why:** generic saving tips are easy to ignore; tips built on your own numbers aren't.
- **Shown in context.** Suggestions appear on the quick report and next to the relevant graph, at most three at a time. **Why:** a long list of tips is homework; a few relevant ones are help.
- **Reason and estimate.** Each suggestion says which data it's based on and an estimated saving, marked as an estimate. **Why:** people trust advice they can check.
- **Feedback.** Users can mark a suggestion "Helpful", "Not relevant" or "Already doing this". **Why:** it improves future suggestions and lets users clear tips they don't need.
- **Generated on the server.** Suggestions are generated by the backend from aggregated usage and weather data, not raw photos or personal details. **Why:** it keeps the app light and limits what personal data goes to an AI model.
- **Weekly refresh.** Suggestions refresh once a week, or after a big change in usage. **Why:** advice that changes every time you open the app feels random.

### 8. Gas ordering and advertising

- **Order gas.** From the gas tracker or a low-gas reminder, users tap "Order gas" and pick a supplier near them. The order is placed through the supplier's channel (in-app request, WhatsApp, phone or website) with the cylinder size and address filled in. **Why:** the moment the app predicts a run-out is the moment people want to fix it.
- **Supplier directory.** A list of LPG suppliers by area with cylinder sizes, prices and delivery options, managed from the backend. **Why:** suppliers and prices change; they can't be hard-coded into the app.
- **Order history.** Orders are logged against the household and, when the cylinder arrives, turn into a cylinder swap with one tap. **Why:** it closes the loop between ordering and tracking without extra typing.
- **Buy electricity shortcut.** The buy-electricity reminder links to the user's preferred prepaid vendor (bank app, retailer or vending site). **Why:** it saves a step without the app having to handle payments.
- **Sponsored suppliers.** Partner suppliers can be featured at the top of the directory, clearly labelled "Sponsored". **Why:** it's the most relevant form of advertising for this app and a direct source of income.
- **Adverts.** Banner or native adverts appear on the reports, history and gas directory screens, never on the scanner, entry or onboarding screens. **Why:** adverts pay for hosting, but they must not slow down logging (see Principles).
- **Ad consent and limits.** Users are asked for consent where the law requires it, and ads are limited to home-related categories where the ad network allows it. **Why:** it keeps the app within Play Store and privacy rules and keeps ads relevant.

### 9. Settings and profile

- **Profile.** Edit name, photo and contact details; see the households you belong to. **Why:** people need to control what other members see about them.
- **Household settings.** Edit household name, address (for supplier search), meters, tariffs and members; regenerate the join code. **Why:** owners need one place to manage a household.
- **OCR model.** Choose between ML Kit and the downloadable slashed-zero and 7-segment models. **Why:** already built, and it makes scanning work on more meter types.
- **Units and currency.** Choose units (kWh, m³, L, kg) and currency, defaulting to rand. **Why:** the app should read naturally for South African households first, without ruling out others.
- **Privacy and data.** Download all my data, delete my account, manage ad consent, read the privacy policy. **Why:** required by POPIA and Google Play, and a sign the app respects its users.
- **Theme.** Light, dark or follow system. **Why:** expected in any modern app and easy to support with Material 3.

---

## 5. Screens

### Screen list

| # | Screen | Purpose | Status |
| --- | --- | --- | --- |
| 1 | Splash | App load and sign-in check | New |
| 2 | Welcome carousel | Three screens: log, predict, share | New |
| 3 | Sign up / Sign in | Google or email account | New |
| 4 | Create profile | Name and photo | New |
| 5 | Create or join household | Choose path | New |
| 6 | Create household | Name, address, utilities tracked | New |
| 7 | Join household | Enter code or scan QR; pending approval state | New |
| 8 | Meter setup | Meter type, prepaid or postpaid, first reading, tariff | New |
| 9 | Home dashboard | Utility cards, quick report, alerts, household switcher | Update (`MeterSelectionScreen`) |
| 10 | Scanner | Camera scan for electricity and water | Existing (`CameraScreen`) |
| 11 | Manual entry | Type a reading, sense checks | Existing (`EnterElectricityReading`) |
| 12 | Log top-up | Amount paid and units received | New |
| 13 | Gas cylinder tracker | Cylinders, days left, swap, order shortcut | Existing (`GasCylinderTrackerScreen`) |
| 14 | Order gas | Supplier list, sponsored suppliers, order details | New |
| 15 | Order confirmation and history | Order status, mark as delivered | New |
| 16 | Quick report | One-card monthly summary with AI suggestions | New |
| 17 | Full report | Projections, graph, readings, anomalies | Update (`ReportsScreen`) |
| 18 | Custom period prediction | Pick a date range, see expected usage and cost | New |
| 19 | Reading history | All readings and events, edit and delete | Update (`EntriesHistoryScreen`) |
| 20 | Reading detail | Reading, photo, weather breakdown, who logged it | New |
| 21 | Export | Choose period, format (PDF or CSV), share or link | New |
| 22 | AI suggestions | Full list of suggestions with feedback | New |
| 23 | Household members | Members, roles, join requests, invite code and QR | New |
| 24 | Activity feed | Household timeline | New |
| 25 | Notifications centre | Past alerts and reminders | New |
| 26 | Notification settings | Per-alert toggles, thresholds, quiet hours | New |
| 27 | Settings | Profile, households, units, theme, OCR model | Update (`SettingsScreen`) |
| 28 | Privacy and account | Data download, account deletion, ad consent, policy | New |
| 29 | Camera permission | Explain and request camera access | Existing (`NoPermissionScreen`) |
| 30 | Offline / sync status | Shows readings waiting to sync | New (banner, not a full screen) |

### Main navigation

The app uses a bottom navigation bar with four tabs, plus a household switcher at the top of Home.

| Tab | Contains |
| --- | --- |
| **Home** | Dashboard, quick logging, quick report, alerts |
| **Reports** | Full reports, predictions, history, export |
| **Household** | Members, activity feed, gas ordering |
| **Settings** | Profile, notifications, privacy, app settings |

### Key flows

```text
First launch
Welcome → Sign up → Profile → Create household → Meter setup → Home
                                └→ Join with code → Waiting for approval → Home

Log a reading (target: under 10 seconds)
Home → tap utility card → Scanner → reading detected → Save → Snackbar (Undo) → Home

Prepaid electricity running low
Push reminder → Home (alert card) → Buy electricity (vendor link) → Log top-up → Run-out date updates for all members

Gas running low
Push reminder → Gas tracker → Order gas → Supplier → Order sent → Mark delivered → New cylinder in tracker
```

---

## 6. Platform and architecture

### Moving from local to online first

| Concern | Today | Next |
| --- | --- | --- |
| Source of truth | Room database on the phone | Database behind the API on EC2 |
| Phone storage | Room (all data) | Room as an offline cache and outbox for unsynced readings |
| Files | App cache folder | S3 (meter photos, exports, profile photos) |
| Background work | WorkManager anomaly worker on the phone | Scheduled jobs on the server; WorkManager only for syncing |
| Notifications | Local notifications | Push notifications (Firebase Cloud Messaging) sent by the server |
| Identity | None | Accounts and household membership checked on every request |

### Components

- **Android app.** Jetpack Compose, MVVM with `StateFlow`, Hilt, Room, CameraX and ML Kit, as today. A new data layer syncs with the API, and repositories read from the local cache first.
- **API on Amazon EC2.** A REST API for accounts, households, readings, events, reports, suggestions and orders. Runs behind a load balancer with HTTPS.
- **Database.** Stores users, households, members, meters, readings, events, orders and suggestions. The choice of database is open (see Open questions).
- **Amazon S3.** Private buckets for meter photos, exports and profile photos. The app uploads and downloads through short-lived pre-signed URLs, so files are never public.
- **Scheduled jobs.** Run on the server: projections and run-out dates after each new reading, daily reminder checks, weekly AI suggestions, monthly summaries.
- **AI service.** Generates suggestions from aggregated usage and weather data.
- **Push notifications.** Firebase Cloud Messaging delivers reminders and alerts to every member's device.

### Sync rules

- Each reading gets an ID on the phone when it's created, so retries never create duplicates.
- Readings saved offline go into an outbox and upload in order when the connection returns.
- If two members edit the same reading, the latest edit wins and both edits appear in the activity feed.
- Projections are calculated on the server so every member sees the same numbers.

### Security and privacy

- All traffic over HTTPS; every API request checks the user's role in the household.
- S3 buckets are private; access is by pre-signed URL only.
- Join codes are random, can be regenerated, and require approval.
- Data handling follows POPIA (South Africa) and Google Play's User Data policy.

---

## 7. Play Store readiness

These are needed before the app can go on the Play Store, separate from new features.

- **Rename the package.** Move from `de.yanneckreiss.mlkittutorial` / `de.yanneckreiss.cameraxtutorial` to an ID you own (for example `za.co.housekeeper.app`). This can't be changed after the first upload.
- **One name.** Pick "HouseKeeper" or "House Check" and use it everywhere: app name, store listing, icon and README.
- **Remove sample data** from release builds; keep it in debug builds only.
- **Target SDK.** Target the API level Google Play currently requires for new apps.
- **Signing.** Use Play App Signing and keep the upload key safe.
- **Privacy policy.** Publish a policy covering accounts, household sharing, photos, location (for supplier search), AI processing and ads.
- **Data safety form.** Declare every type of data collected and shared, including the ad SDK's.
- **Account deletion.** In-app and through a web link.
- **Ads declaration.** Declare that the app contains ads; follow Play's ads and families policies.
- **Testing track.** New personal developer accounts must run a closed test with a minimum number of testers before applying for production access; family and friends can be the first testers.
- **Store listing.** Icon, feature graphic, screenshots (the ones in `docs/screenshots` are a good start) and a short description.
- **Crash reporting and analytics** (for example Firebase Crashlytics), declared in the data safety form.

---

## 8. Out of scope

Not part of this version:

- iOS app or web app
- Paying for electricity or gas inside the app (orders and purchases go through suppliers and vendors)
- Reading smart meters or connecting to municipal or Eskom accounts automatically
- Bill payments, debit orders or managing municipal accounts
- Splitting bills or tracking who owes whom between members
- Live location sharing between household members (only the join-code pattern is borrowed from Life360)
- In-app chat between members
- Landlord or property-manager dashboards for many properties
- Supplier-side app for managing orders and deliveries
- Smart home control (turning appliances or geysers on and off)
- Solar, inverter and battery tracking
- Water quality, refuse or other non-metered services
- Languages other than English
- A subscription or paid tier to remove ads

---

## 9. Open questions

| # | Question | Notes |
| --- | --- | --- |
| 1 | Which database sits behind the API? | Options include PostgreSQL on Amazon RDS, a database on the same EC2 instance, or DynamoDB. RDS is the safest choice for backups. |
| 2 | How is sign-in handled? | Amazon Cognito, Firebase Authentication, or a custom service on EC2. |
| 3 | Which AI model generates suggestions, and what does it cost per household per month? | Affects whether suggestions are weekly or on demand. |
| 4 | Which ad network? | AdMob is the default on Android; check category controls and consent tools. |
| 5 | How do gas suppliers receive orders in v1? | WhatsApp or phone handoff is fastest to launch; an in-app order queue needs supplier sign-up. |
| 6 | How many members can a household have? | A limit (for example 10) keeps notifications and costs predictable. |
| 7 | What happens to a household when its owner deletes their account? | Transfer ownership to an admin, or delete the household after notice. |
| 8 | Final app name: HouseKeeper or House Check? | Needed before the store listing and package rename. |

---

*Template note: the Notion page linked as a template only loads with JavaScript, so its contents couldn't be read. This document follows a standard Notion-style spec layout (properties table, callout summary, contents, sections) and the feature format you supplied.*
