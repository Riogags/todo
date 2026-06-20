package com.lyricvideo.ui;

import com.lyricvideo.model.LyricLine;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * LyricSequencer — shows one popup at a time and waits for the user
 * to click OK before advancing to the next lyric.
 */
public class LyricSequencer {

    private static final int FULL_LINE_FONT = 16;
    private static final int WORD_FONT      = 20;

    private final Frame           owner;
    private final List<LyricLine> lyrics;

    public LyricSequencer(Frame owner, List<LyricLine> lyrics) {
        this.owner  = owner;
        this.lyrics = lyrics;
    }

    /** Start the sequence on the EDT. Each modal popup blocks until OK is clicked. */
    public void start() {
        SwingUtilities.invokeLater(this::runSequence);
    }

    private void runSequence() {
        for (LyricLine line : lyrics) {
            if (line.isWordByWord()) {
                for (String word : line.getText().trim().split("\\s+")) {
                    showPopup(word, WORD_FONT);
                }
            } else {
                showPopup(line.getText(), FULL_LINE_FONT);
            }
        }
    }

    private void showPopup(String text, int fontSize) {
        JLabel message = new JLabel("<html>" + text + "</html>");
        message.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        JOptionPane.showMessageDialog(owner, message, "Nude", JOptionPane.INFORMATION_MESSAGE);
    }
}
