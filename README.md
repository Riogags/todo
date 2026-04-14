# PowerPal

Crowd-sourced outage logging with timely "charge now" alerts.

> **MVP, hackathon scope.** See [`docs/powerpal.md`](docs/powerpal.md) for the full concept.

## What this is

A small Node + Express + SQLite app with a mobile-first web UI. Users log
outages with one tap; a per-area statistical model computes the next likely
outage window and typical duration. Claude (optional) generates a friendly
natural-language summary when an `ANTHROPIC_API_KEY` is configured.

## Quick start

```bash
npm install
npm start
# -> http://localhost:3000
```

Set a port or DB path via environment variables if needed:

```bash
PORT=4000 POWERPAL_DB_PATH=./data/powerpal.db npm start
```

### Optional: AI summaries

Export an Anthropic API key before starting the server:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
npm start
```

The frontend auto-enables the "Explain with AI" button when AI is available.

## API

All endpoints return JSON.

| Method | Path                                    | Purpose                                |
| ------ | --------------------------------------- | -------------------------------------- |
| GET    | `/api/health`                           | Server status + AI availability flag   |
| GET    | `/api/areas`                            | List areas that have any events        |
| POST   | `/api/events`                           | Log an event (see schema below)        |
| GET    | `/api/areas/:area/events?limit=N`       | Recent events for an area              |
| GET    | `/api/areas/:area/status`               | Is power currently on or off?          |
| GET    | `/api/areas/:area/prediction`           | Stats + next likely outage             |
| GET    | `/api/areas/:area/ai-summary`           | Claude-generated explanation (opt-in)  |

### Event schema

```json
{
  "area": "Yaba, Lagos",
  "type": "outage_start",      // or "power_restored"
  "ts": 1733328000000,         // optional; defaults to server time
  "reporter": "anon-1234"      // optional; arbitrary string <=64 chars
}
```

## Project layout

```
src/
  server.js     Express routes
  db.js         SQLite schema + queries
  predict.js    Pair events into outages; compute histograms & forecast
  claude.js     Optional Anthropic SDK integration
public/
  index.html    Mobile-first UI
  styles.css
  app.js        Single-file vanilla-JS client
docs/
  powerpal.md   Concept doc
```

## Notes

- The SQLite database lives at `data/powerpal.db` by default and is
  `.gitignore`d.
- The prediction is intentionally statistical, not ML. Claude is used only for
  *explanation*, never for the raw forecast.
- There is no auth in this MVP. Area names are the only identifier, and events
  are stored unauthenticated. Treat this as a demo, not production.
