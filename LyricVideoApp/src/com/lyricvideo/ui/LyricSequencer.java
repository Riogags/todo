package com.lyricvideo.ui;

import com.lyricvideo.model.LyricLine;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

/**
 * LyricSequencer — drives the timed display of lyric popups.
 *
 * Each popup is a standard non-modal JDialog whose content is built with
 * JOptionPane, so it inherits the native OS look and feel automatically.
 *
 * Threading model: the timing loop runs on a daemon thread; all Swing
 * operations are dispatched to the EDT via invokeAndWait / invokeLater.
 */
public class LyricSequencer {

    // ── Tweak these to change the overall feel ────────────────────────────────

    /** Max random pixel offset from screen centre per popup. */
    private static final int POSITION_JITTER_PX = 65;

    /** Max random ms added/subtracted from each preDelayMs (set 0 to disable). */
    private static final int TIMING_JITTER_MS   = 180;

    /** Blank gap (ms) between consecutive popups. */
    private static final int INTER_POPUP_GAP_MS  = 80;

    /** Font size for full-line entries. */
    private static final int FULL_LINE_FONT = 16;

    /** Font size for word-by-word entries (larger = more dramatic). */
    private static final int WORD_FONT      = 20;

    // ─────────────────────────────────────────────────────────────────────────

    private final Frame           owner;
    private final List<LyricLine> lyrics;
    private final Random          random = new Random();

    /** The currently visible dialog; null when nothing is shown. EDT access only. */
    private JDialog currentDialog;

    public LyricSequencer(Frame owner, List<LyricLine> lyrics) {
        this.owner  = owner;
        this.lyrics = lyrics;
    }

    /** Start the sequence on a background daemon thread. Returns immediately. */
    public void start() {
        Thread t = new Thread(this::runSequence, "LyricSequencer");
        t.setDaemon(true);
        t.start();
    }

    // ── Sequence loop ─────────────────────────────────────────────────────────

    private void runSequence() {
        try {
            for (LyricLine line : lyrics) {
                if (Thread.currentThread().isInterrupted()) break;

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
            Thread.currentThread().interrupt();
        } finally {
            SwingUtilities.invokeLater(this::closeCurrentDialog);
        }
    }

    private void runFullLine(LyricLine line) throws InterruptedException {
        showPopup(line.getText(), FULL_LINE_FONT, line.getDisplayDurationMs());
    }

    private void runWordByWord(LyricLine line) throws InterruptedException {
        for (String word : line.getText().trim().split("\\s+")) {
            if (Thread.currentThread().isInterrupted()) return;
            showPopup(word, WORD_FONT, line.getDisplayDurationMs());
            int gapJitter = random.nextInt(81) - 40;
            Thread.sleep(Math.max(0, line.getWordDelayMs() + gapJitter));
        }
    }

    // ── Popup lifecycle ───────────────────────────────────────────────────────

    private void showPopup(String text, int fontSize, int durationMs)
            throws InterruptedException {

        try {
            SwingUtilities.invokeAndWait(() -> {
                closeCurrentDialog();

                // JLabel as message so we can set a larger font for readability
                JLabel message = new JLabel("<html>" + text + "</html>");
                message.setFont(new Font("Dialog", Font.PLAIN, fontSize));

                JOptionPane pane = new JOptionPane(
                        message,
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION
                );

                currentDialog = new JDialog(owner, "Nude", false); // non-modal
                currentDialog.setContentPane(pane);
                currentDialog.pack();
                positionWithJitter(currentDialog);
                currentDialog.setVisible(true);
                currentDialog.toFront();
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to show popup", e.getCause());
        }

        Thread.sleep(durationMs);
        SwingUtilities.invokeLater(this::closeCurrentDialog);
        Thread.sleep(INTER_POPUP_GAP_MS);
    }

    /** Centre on screen with a small random offset for an organic feel. */
    private void positionWithJitter(JDialog dialog) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int baseX = (screen.width  - dialog.getWidth())  / 2;
        int baseY = (screen.height - dialog.getHeight()) / 2;
        int dx = random.nextInt(POSITION_JITTER_PX * 2 + 1) - POSITION_JITTER_PX;
        int dy = random.nextInt(POSITION_JITTER_PX * 2 + 1) - POSITION_JITTER_PX;
        int x  = Math.max(0, Math.min(screen.width  - dialog.getWidth(),  baseX + dx));
        int y  = Math.max(0, Math.min(screen.height - dialog.getHeight(), baseY + dy));
        dialog.setLocation(x, y);
    }

    /** Hide and dispose the current dialog. Must be called on the EDT. */
    private void closeCurrentDialog() {
        if (currentDialog != null) {
            currentDialog.setVisible(false);
            currentDialog.dispose();
            currentDialog = null;
        }
    }
}
