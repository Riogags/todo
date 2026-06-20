package com.lyricvideo.ui;

import com.lyricvideo.model.LyricLine;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Shows one popup at a time. Click OK to advance to the next lyric.
 * Passing null to JOptionPane centres the dialog on screen without
 * any owner window behind it.
 */
public class LyricSequencer {

    private static final int FULL_LINE_FONT = 16;
    private static final int WORD_FONT      = 20;

    private final List<LyricLine> lyrics;

    public LyricSequencer(List<LyricLine> lyrics) {
        this.lyrics = lyrics;
    }

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
        // null owner = no background window, dialog floats on screen by itself
        JOptionPane.showMessageDialog(null, message, "Nude", JOptionPane.INFORMATION_MESSAGE);
    }
}
