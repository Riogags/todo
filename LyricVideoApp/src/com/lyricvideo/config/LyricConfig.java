package com.lyricvideo.config;

import com.lyricvideo.model.LyricLine;

import java.util.Arrays;
import java.util.List;

/**
 * Central configuration file: all lyrics and their timing values live here.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  HOW TO FILL IN YOUR LYRICS
 * ═══════════════════════════════════════════════════════════════════
 *
 *  1. Replace every placeholder string (e.g. "[ VERSE 1 — LINE 1 ]")
 *     with the actual lyric line you want displayed.
 *
 *  2. Adjust preDelayMs (the pause AFTER the previous popup disappears
 *     and BEFORE this popup appears):
 *       - 0     → appears immediately after previous popup
 *       - 500   → half-second gap
 *       - 2000  → two-second gap (good for long instrumental breaks)
 *
 *  3. Adjust displayDurationMs (how long the popup stays on screen).
 *     A rough guide at the song's BPM:
 *       - One beat at  72 BPM ≈ 833 ms
 *       - One beat at  80 BPM ≈ 750 ms
 *       - One beat at 120 BPM ≈ 500 ms
 *     Most sung phrases land between 2 and 5 beats.
 *
 *  4. Set wordByWord = true for lines you want to appear word-by-word.
 *     Each word gets its own popup. Set wordDelayMs to control the gap
 *     between consecutive word-popups (try 120–250 ms for a staccato feel).
 *
 * ═══════════════════════════════════════════════════════════════════
 *  TIMING WORKFLOW
 * ═══════════════════════════════════════════════════════════════════
 *
 *  Option A — adjust-and-re-record (easiest):
 *    Run the app, record the screen, add music in your editor,
 *    then nudge preDelayMs values and re-record until sync is tight.
 *
 *  Option B — pre-calculate from a lyrics sheet:
 *    Open the song in Audacity or a DAW, note the timestamp (ms) of
 *    each sung word, then compute:
 *      preDelayMs[n] = timestamp[n] - (timestamp[n-1] + displayDurationMs[n-1])
 *
 *  Note: LyricSequencer adds ±180 ms random jitter to preDelayMs
 *  automatically for an organic feel. Disable it by setting
 *  TIMING_JITTER_MS = 0 in LyricSequencer.java.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class LyricConfig {

    /**
     * Returns the ordered list of lyric entries.
     *
     * <p>Entries play top-to-bottom. The sequence begins the instant the
     * program starts, so the first {@code preDelayMs} should equal the
     * length of the song's intro in milliseconds.
     */
    public static List<LyricLine> getLyrics() {
        return Arrays.asList(

            // ┌─────────────────────────────────────────────────────────┐
            // │  INTRO  (instrumental)                                  │
            // │  Set preDelayMs to the intro length in milliseconds.    │
            // │  E.g. if vocals start at 0:08 → preDelayMs = 8000      │
            // └─────────────────────────────────────────────────────────┘

            // ── VERSE 1 ────────────────────────────────────────────────

            new LyricLine(
                // ▼ REPLACE: type Verse 1, line 1 between these quotes ▼
                "[ VERSE 1 — LINE 1 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 8000   // ← set to intro length
            ),

            new LyricLine(
                "[ VERSE 1 — LINE 2 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ VERSE 1 — LINE 3 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ VERSE 1 — LINE 4 ]",
                /* displayDurationMs */ 3800,
                /* preDelayMs        */ 400
            ),

            // ── PRE-CHORUS ─────────────────────────────────────────────
            // Word-by-word example: each word pops up separately.
            // Try wordDelayMs 120–200 ms for a staccato / typewriter effect.

            new LyricLine(
                "[ PRE-CHORUS — LINE 1 ]",
                /* displayDurationMs */ 420,
                /* preDelayMs        */ 900,
                /* wordByWord        */ true,
                /* wordDelayMs       */ 160
            ),

            new LyricLine(
                "[ PRE-CHORUS — LINE 2 ]",
                /* displayDurationMs */ 3400,
                /* preDelayMs        */ 500
            ),

            // ── CHORUS ────────────────────────────────────────────────

            new LyricLine(
                "[ CHORUS — LINE 1 ]",
                /* displayDurationMs */ 3600,
                /* preDelayMs        */ 1000
            ),

            new LyricLine(
                "[ CHORUS — LINE 2 ]",
                /* displayDurationMs */ 3600,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ CHORUS — LINE 3 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ CHORUS — LINE 4 ]",
                /* displayDurationMs */ 4200,
                /* preDelayMs        */ 400
            ),

            // ── VERSE 2 ───────────────────────────────────────────────
            // Longer preDelayMs here if there's an instrumental bar between chorus and verse 2

            new LyricLine(
                "[ VERSE 2 — LINE 1 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 1800
            ),

            new LyricLine(
                "[ VERSE 2 — LINE 2 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ VERSE 2 — LINE 3 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ VERSE 2 — LINE 4 ]",
                /* displayDurationMs */ 3800,
                /* preDelayMs        */ 400
            ),

            // ── PRE-CHORUS (repeat) ────────────────────────────────────

            new LyricLine(
                "[ PRE-CHORUS — LINE 1 ]",
                /* displayDurationMs */ 420,
                /* preDelayMs        */ 900,
                /* wordByWord        */ true,
                /* wordDelayMs       */ 160
            ),

            new LyricLine(
                "[ PRE-CHORUS — LINE 2 ]",
                /* displayDurationMs */ 3400,
                /* preDelayMs        */ 500
            ),

            // ── CHORUS (repeat) ───────────────────────────────────────
            // First chorus line word-by-word for variety; rest as full lines.

            new LyricLine(
                "[ CHORUS — LINE 1 ]",
                /* displayDurationMs */ 480,
                /* preDelayMs        */ 1000,
                /* wordByWord        */ true,
                /* wordDelayMs       */ 200
            ),

            new LyricLine(
                "[ CHORUS — LINE 2 ]",
                /* displayDurationMs */ 3600,
                /* preDelayMs        */ 500
            ),

            new LyricLine(
                "[ CHORUS — LINE 3 ]",
                /* displayDurationMs */ 3200,
                /* preDelayMs        */ 400
            ),

            new LyricLine(
                "[ CHORUS — LINE 4 ]",
                /* displayDurationMs */ 4500,
                /* preDelayMs        */ 400
            ),

            // ── BRIDGE ────────────────────────────────────────────────
            // Often slower / more spacious — increase preDelayMs to taste.

            new LyricLine(
                "[ BRIDGE — LINE 1 ]",
                /* displayDurationMs */ 4000,
                /* preDelayMs        */ 3000   // instrumental break before bridge
            ),

            new LyricLine(
                "[ BRIDGE — LINE 2 ]",
                /* displayDurationMs */ 4000,
                /* preDelayMs        */ 600
            ),

            new LyricLine(
                "[ BRIDGE — LINE 3 ]",
                /* displayDurationMs */ 4200,
                /* preDelayMs        */ 600
            ),

            // ── OUTRO ─────────────────────────────────────────────────
            // Long displayDurationMs lets it fade naturally at the song's end.

            new LyricLine(
                "[ OUTRO — FINAL LINE ]",
                /* displayDurationMs */ 7000,
                /* preDelayMs        */ 2000
            )

        );
    }
}
