package com.lyricvideo.model;

/**
 * Immutable data record describing a single entry in the lyric timeline.
 *
 * <p>Each LyricLine maps to one or more retro popup dialogs:
 * <ul>
 *   <li>If {@code wordByWord} is {@code false}: one popup shows the entire
 *       {@code text} string for {@code displayDurationMs} milliseconds.</li>
 *   <li>If {@code wordByWord} is {@code true}: each space-delimited word
 *       gets its own popup, visible for {@code displayDurationMs} ms, with
 *       a {@code wordDelayMs} gap between consecutive word-popups.</li>
 * </ul>
 *
 * <h3>Timing model</h3>
 * <pre>
 *   ... [previous popup ends] → sleep(preDelayMs) → [this popup appears] → sleep(displayDurationMs) → [popup hides] ...
 * </pre>
 *
 * Edit {@link com.lyricvideo.config.LyricConfig} to set the values for your song.
 */
public final class LyricLine {

    private final String  text;
    private final int     displayDurationMs;
    private final int     preDelayMs;
    private final boolean wordByWord;
    private final int     wordDelayMs;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor — use this for word-by-word entries.
     *
     * @param text               Lyric text (may contain spaces for multi-word lines)
     * @param displayDurationMs  How long (ms) each popup (or per-word popup) stays visible
     * @param preDelayMs         Silence (ms) after the previous popup disappears before
     *                           this entry begins
     * @param wordByWord         {@code true} → each word gets its own popup
     * @param wordDelayMs        Gap (ms) between consecutive word-popups (ignored when
     *                           {@code wordByWord} is {@code false})
     */
    public LyricLine(String text, int displayDurationMs, int preDelayMs,
                     boolean wordByWord, int wordDelayMs) {
        this.text              = text;
        this.displayDurationMs = displayDurationMs;
        this.preDelayMs        = preDelayMs;
        this.wordByWord        = wordByWord;
        this.wordDelayMs       = wordDelayMs;
    }

    /**
     * Convenience constructor for entries that display as a complete line.
     *
     * @param text               The full lyric line to display in one popup
     * @param displayDurationMs  How long (ms) the popup stays visible
     * @param preDelayMs         Pause (ms) after the previous popup disappears
     */
    public LyricLine(String text, int displayDurationMs, int preDelayMs) {
        this(text, displayDurationMs, preDelayMs, false, 0);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** The lyric text to display (full line, or space-separated words for word-by-word). */
    public String getText()              { return text; }

    /** Milliseconds each popup (or each per-word popup) remains on screen. */
    public int getDisplayDurationMs()    { return displayDurationMs; }

    /** Milliseconds of silence between the previous popup ending and this entry starting. */
    public int getPreDelayMs()           { return preDelayMs; }

    /** Whether each word in {@code text} should appear as its own popup. */
    public boolean isWordByWord()        { return wordByWord; }

    /** Gap (ms) between consecutive per-word popups. Only relevant when {@code wordByWord} is true. */
    public int getWordDelayMs()          { return wordDelayMs; }
}
