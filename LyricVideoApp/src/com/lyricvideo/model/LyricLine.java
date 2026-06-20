package com.lyricvideo.model;

/**
 * A single lyric entry — just the text and whether to show it word-by-word.
 * Advancement is manual (OK button), so no timing fields are needed.
 */
public final class LyricLine {

    private final String  text;
    private final boolean wordByWord;

    /**
     * @param text        The lyric text to display
     * @param wordByWord  true → each word gets its own popup
     */
    public LyricLine(String text, boolean wordByWord) {
        this.text       = text;
        this.wordByWord = wordByWord;
    }

    /** Convenience constructor for full-line entries. */
    public LyricLine(String text) {
        this(text, false);
    }

    public String  getText()      { return text; }
    public boolean isWordByWord() { return wordByWord; }
}
