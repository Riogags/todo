package com.lyricvideo.config;

import com.lyricvideo.model.LyricLine;

import java.util.Arrays;
import java.util.List;

/**
 * All your lyrics go here — one new LyricLine(...) per popup.
 *
 * Usage:
 *   new LyricLine("your line here")           → whole line in one popup
 *   new LyricLine("your line here", true)     → one popup per word
 *
 * Click OK on each popup to advance to the next line.
 */
public class LyricConfig {

    public static List<LyricLine> getLyrics() {
        return Arrays.asList(

            // ── REPLACE each placeholder with a real lyric line ──────────────
            // Full line examples:
            new LyricLine("[ VERSE 1 — LINE 1 ]"),
            new LyricLine("[ VERSE 1 — LINE 2 ]"),
            new LyricLine("[ VERSE 1 — LINE 3 ]"),
            new LyricLine("[ VERSE 1 — LINE 4 ]"),

            // Word-by-word example (add , true to any line):
            new LyricLine("[ PRE-CHORUS LINE ]", true),

            new LyricLine("[ CHORUS — LINE 1 ]"),
            new LyricLine("[ CHORUS — LINE 2 ]"),
            new LyricLine("[ CHORUS — LINE 3 ]"),
            new LyricLine("[ CHORUS — LINE 4 ]"),

            new LyricLine("[ VERSE 2 — LINE 1 ]"),
            new LyricLine("[ VERSE 2 — LINE 2 ]"),
            new LyricLine("[ VERSE 2 — LINE 3 ]"),
            new LyricLine("[ VERSE 2 — LINE 4 ]"),

            new LyricLine("[ CHORUS — LINE 1 ]"),
            new LyricLine("[ CHORUS — LINE 2 ]"),
            new LyricLine("[ CHORUS — LINE 3 ]"),
            new LyricLine("[ CHORUS — LINE 4 ]"),

            new LyricLine("[ BRIDGE — LINE 1 ]"),
            new LyricLine("[ BRIDGE — LINE 2 ]"),

            new LyricLine("[ OUTRO — FINAL LINE ]")
            // ── No comma after the last entry ────────────────────────────────

        );
    }
}
