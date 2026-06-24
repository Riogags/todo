# PowerPal — Turning Unpredictable Power Into Predictable Action

**Hackathon MVP concept**

---

## 1. The Problem

It's 6pm. Your phone is at 8%, dinner is half-cooked, and you have no idea when the next outage starts — or how long it will last. For millions of people on unreliable grids, this is every day.

Two overlapping problems drive the pain:

- **Scheduled load shedding** — utility-announced but frequently changed, published in formats that are hard to parse, and often inaccurate at the street level.
- **Unplanned outages** — faults, overloads, and weather events that nobody warns you about.

The cost is not just inconvenience. People miss work calls, lose refrigerated food, can't study or run small businesses, and waste money on fuel for generators they don't always need.

## 2. The Solution (One Sentence)

**PowerPal turns crowd-reported outage data into timely "charge now" alerts, so users can plan instead of guess.**

## 3. How It Works

Three layers, intentionally simple:

1. **Crowd-sourced outage logs.** Users tap a single button when power goes off and again when it comes back on. Each event is stamped with a timestamp and a coarse area (neighborhood, not precise GPS).
2. **Statistical prediction per area.** Rolling-window frequency, time-of-day histograms, and typical-duration estimates produce a forecast for each area. No machine learning is needed at MVP — the patterns in load shedding are already statistical in nature.
3. **Claude as the explanation layer.** The Claude API is used where it shines: generating human-readable summaries ("Based on the last 7 days in your area, expect an outage around 6pm lasting ~2 hours") and answering user questions ("Why am I getting this alert?"). This is cheaper, faster, and more reliable than asking an LLM to predict.

This split is deliberate. It keeps the core prediction loop transparent and debuggable, and it keeps API costs bounded while still giving the product a conversational, intelligent feel.

## 4. Core Features

1. **Outage Logging** — one-tap ON/OFF reporting.
2. **Area Predictions** — next likely outage window and expected duration.
3. **Smart Alerts** — "Charge now," "Power restored," "Likely outage in 30 min."
4. **Area Intelligence Dashboard** — aggregated, anonymized view of reliability in your neighborhood.
5. **SMS Interaction Mode** — for users on feature phones or during data outages.

## 5. System Architecture

1. **Frontend:** mobile-first web interface (progressive web app for offline-friendly logging).
2. **Backend:** API server with a database of timestamped events + coarse location.
3. **Prediction service:** lightweight statistical model running per area, refreshed periodically.
4. **AI layer:** Claude API for natural-language summaries and user Q&A — **not** the primary prediction engine.
5. **Notification layer:** push notifications, with SMS as fallback via Twilio or Africa's Talking.

## 6. Data Quality & Privacy

This is where most crowd-sourced products fail. PowerPal takes it seriously from day one.

- **Signal vs. noise.** A single report is a hint, not a truth. An outage is only "confirmed" for an area when it crosses a corroboration threshold (e.g., ≥3 users within a time window). This filters out dead phones and forgotten taps.
- **Confirmation prompts.** After a suspected outage ends, users are asked "Was the power actually out?" to refine the dataset.
- **Coarse location only.** Area-level (neighborhood or grid block), never precise GPS. Users choose their area on setup.
- **Opt-in data sharing.** Aggregation for dashboards is opt-in; individual timestamps are never shown to other users.
- **Data minimization.** No names, no device IDs tied to public data, no tracking beyond what's needed for predictions.

## 7. Cold-Start Strategy

New products in new areas have no data. PowerPal bootstraps with:

- **Published utility schedules** where they exist (e.g., Eskom load-shedding stages, EKEDC schedules). These seed baseline predictions on day one.
- **Corroboration thresholds** before publishing predictions to users — if an area has fewer than *N* active reporters, it shows "Not enough data yet" rather than a bad guess.
- **Ambassador seeding** — partner with a handful of power-user reporters per launch city to bootstrap coverage.

## 8. Business Model

Ordered from strongest to weakest:

1. **B2B partnerships (primary).** Inverter, solar, and battery companies pay for qualified leads in high-outage areas and for anonymized, aggregated demand data. This is the clearest willingness-to-pay.
2. **Utility and municipal contracts.** Reliability dashboards and customer-communication tooling for utilities that want to understand outage reports from their network.
3. **Premium consumer features (secondary).** Advanced predictions, appliance-level battery optimization, outage history exports for claims.
4. **Freemium base.** Free tier with core alerts, to drive the data flywheel.

## 9. Competitive Advantage

1. **Local, not global.** Focused on the grid realities of emerging markets, not a generic utility app.
2. **Action-based, not data-based.** "Charge now" beats "here's a chart."
3. **Works in low bandwidth.** SMS mode keeps the product useful exactly when competitors fail — during outages.
4. **Crowd flywheel.** Each new user improves predictions for everyone in their area.

## 10. Demo Flow

1. User logs an outage.
2. Event is stored and contributes to the area's dataset.
3. Prediction service updates the area forecast.
4. User receives a "Charge now" alert for the next predicted outage window.
5. As more users in the area log events, predictions sharpen — visibly, within the demo.

## 11. The Pitch

> Every day, millions of people lose a few hours — and a few percent of their phone batteries — to outages nobody warned them about. PowerPal turns unpredictable electricity into predictable action. Instead of guessing, users know when to charge, when to cook, and when to save. One tap creates data. The data creates predictions. The predictions create calm.
