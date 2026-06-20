package com.lyricvideo.ui;

import com.lyricvideo.model.LyricLine;

import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

/**
 * LyricSequencer — Drives the timed display of lyric popup dialogs.
 *
 * <h3>Threading model</h3>
 * <ul>
 *   <li>The timing loop runs on a dedicated <em>daemon</em> background thread
 *       so that {@link Thread#sleep} never blocks the Swing Event Dispatch Thread
 *       (which would freeze the UI).</li>
 *   <li>All Swing operations (create / show / hide dialogs) are dispatched via
 *       {@link SwingUtilities#invokeAndWait} (blocking) or
 *       {@link SwingUtilities#invokeLater} (fire-and-forget).</li>
 * </ul>
 *
 * <h3>Timing model per LyricLine</h3>
 * <pre>
 *  ─── previous popup hides ──► sleep(preDelayMs ± jitter)
 *      ──► popup appears ──► sleep(displayDurationMs) ──► popup hides
 *      ──► sleep(INTER_POPUP_GAP_MS) ──► next entry ...
 * </pre>
 *
 * <h3>Tweakable constants</h3>
 * All global timing/visual knobs are collected at the top of this class.
 * Adjust them to taste without touching the logic below.
 */
public class LyricSequencer {

    // ═══════════════════════════════════════════════════════════════════════
    //  GLOBAL TWEAKS — adjust these to change the overall feel
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Maximum random pixel offset from screen centre applied to each popup.
     * Higher values = more erratic, bouncing-around feel.
     * Recommended range: 30–100.
     */
    private static final int POSITION_JITTER_PX = 65;

    /**
     * Maximum random milliseconds added to (or subtracted from) each
     * {@code preDelayMs} value. This gives the sequence a slightly human,
     * "not perfectly metronomic" quality.
     * Set to 0 to disable jitter and use exact timings from LyricConfig.
     */
    private static final int TIMING_JITTER_MS = 180;

    /**
     * Blank-screen gap (ms) inserted between consecutive popups.
     * A non-zero value ensures each popup has a distinct "arrival" moment
     * even when {@code preDelayMs} in LyricConfig is 0.
     */
    private static final int INTER_POPUP_GAP_MS = 80;

    // ── Dialog size / font for full-line entries ───────────────────────────
    /** Pixel width of full-line lyric popups. */
    private static final int FULL_WIDTH = 400;
    /** Font size (pt) of lyric text in full-line popups. */
    private static final int FULL_FONT  = 16;

    // ── Dialog size / font for word-by-word entries ────────────────────────
    /** Pixel width of single-word popups (narrower, more compact). */
    private static final int WORD_WIDTH = 270;
    /** Font size (pt) for single-word popups (larger, more dramatic). */
    private static final int WORD_FONT  = 22;

    // ═══════════════════════════════════════════════════════════════════════

    private final Frame            owner;
    private final List<LyricLine>  lyrics;
    private final Random           random = new Random();

    /**
     * Currently visible dialog. Accessed only on the EDT; kept as a field
     * so {@link #closeCurrentDialog()} can always reach it.
     */
    private RetroDialog currentDialog;

    // ─────────────────────────────────────────────────────────────────────

    /**
     * @param owner   The BackgroundFrame that owns popup dialogs (keeps them
     *                on top and tied to the app's lifecycle)
     * @param lyrics  Ordered list of lyric entries from {@link com.lyricvideo.config.LyricConfig}
     */
    public LyricSequencer(Frame owner, List<LyricLine> lyrics) {
        this.owner  = owner;
        this.lyrics = lyrics;
    }

    /**
     * Start the lyric sequence on a background daemon thread.
     * Returns immediately; the sequence runs independently of the caller.
     */
    public void start() {
        Thread thread = new Thread(this::runSequence, "LyricSequencer");
        thread.setDaemon(true); // dies automatically when the main window closes
        thread.start();
    }

    // ── Sequence loop ─────────────────────────────────────────────────────

    /** Main loop — iterates over every LyricLine in order. */
    private void runSequence() {
        try {
            for (LyricLine line : lyrics) {
                if (Thread.currentThread().isInterrupted()) break;

                // Apply pre-delay + optional timing jitter
                int jitter   = random.nextInt(TIMING_JITTER_MS * 2 + 1) - TIMING_JITTER_MS;
                int preDelay = Math.max(0, line.getPreDelayMs() + jitter);
                Thread.sleep(preDelay);

                if (line.isWordByWord()) {
                    runWordByWord(line);
                } else {
                    runFullLine(line);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted flag
        } finally {
            // Always hide any leftover popup when the sequence ends or is interrupted
            SwingUtilities.invokeLater(this::closeCurrentDialog);
        }
    }

    // ── Per-entry display strategies ──────────────────────────────────────

    /** Shows the entire lyric text in a single popup. */
    private void runFullLine(LyricLine line) throws InterruptedException {
        showPopup(line.getText(), FULL_WIDTH, FULL_FONT, line.getDisplayDurationMs());
    }

    /**
     * Splits the lyric text on whitespace and shows each token (word) in its
     * own popup, with a configurable gap between consecutive words.
     */
    private void runWordByWord(LyricLine line) throws InterruptedException {
        String[] words = line.getText().trim().split("\\s+");
        for (String word : words) {
            if (Thread.currentThread().isInterrupted()) return;

            showPopup(word, WORD_WIDTH, WORD_FONT, line.getDisplayDurationMs());

            // Inter-word gap (also lightly jittered for organic feel)
            int gapJitter = random.nextInt(81) - 40; // ±40 ms
            Thread.sleep(Math.max(0, line.getWordDelayMs() + gapJitter));
        }
    }

    // ── Popup lifecycle ───────────────────────────────────────────────────

    /**
     * Opens a new popup, blocks the sequencer thread for {@code durationMs},
     * then hides the popup and waits for a brief inter-popup gap.
     *
     * <p>All Swing calls are marshalled to the EDT via {@code invokeAndWait}
     * (so the dialog is guaranteed to be on screen before we sleep) and
     * {@code invokeLater} (for hide, where we don't need to wait).
     *
     * @param text       The text to display in the dialog
     * @param width      Dialog pixel width
     * @param fontSize   Font size for the lyric text
     * @param durationMs How long (ms) to keep the dialog visible
     */
    private void showPopup(String text, int width, int fontSize, int durationMs)
            throws InterruptedException {

        // --- Show on EDT (blocking: wait until dialog is actually visible) ---
        try {
            SwingUtilities.invokeAndWait(() -> {
                closeCurrentDialog(); // dismiss any previous popup first

                currentDialog = new RetroDialog(owner, "Nude", width, fontSize);
                currentDialog.setLyricText(text);
                currentDialog.positionWithJitter(POSITION_JITTER_PX);
                currentDialog.setVisible(true);
                currentDialog.toFront();
            });
        } catch (InvocationTargetException ex) {
            // Propagate as unchecked — nothing sensible to recover from here
            throw new RuntimeException("Failed to display lyric popup", ex.getCause());
        }

        // --- Keep visible --- (sequencer thread sleeps here)
        Thread.sleep(durationMs);

        // --- Hide on EDT (fire-and-forget: we don't need to wait) ---
        SwingUtilities.invokeLater(this::closeCurrentDialog);

        // Brief blank-screen pause before the next popup
        Thread.sleep(INTER_POPUP_GAP_MS);
    }

    /**
     * Hides and disposes the currently visible dialog, if any.
     * <strong>Must be called on the EDT.</strong>
     */
    private void closeCurrentDialog() {
        if (currentDialog != null) {
            currentDialog.setVisible(false);
            currentDialog.dispose();
            currentDialog = null;
        }
    }
}
