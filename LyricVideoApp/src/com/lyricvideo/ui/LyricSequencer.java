package com.lyricvideo.ui;

import com.lyricvideo.model.LyricLine;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Shows a single persistent dialog. The text updates in place each time
 * OK is clicked — the dialog never closes between lyrics.
 */
public class LyricSequencer {

    private static final int FULL_LINE_FONT = 16;
    private static final int WORD_FONT      = 20;

    private final List<LyricLine> lyrics;

    private JLabel lyricLabel;
    private volatile CountDownLatch latch;

    public LyricSequencer(List<LyricLine> lyrics) {
        this.lyrics = lyrics;
    }

    public void start() {
        Thread t = new Thread(this::runSequence, "LyricSequencer");
        t.setDaemon(true);
        t.start();
    }

    private void runSequence() {
        try {
            SwingUtilities.invokeAndWait(this::createDialog);

            for (LyricLine line : lyrics) {
                if (line.isWordByWord()) {
                    for (String word : line.getText().trim().split("\\s+")) {
                        showLyric(word, WORD_FONT);
                    }
                } else {
                    showLyric(line.getText(), FULL_LINE_FONT);
                }
            }
        } catch (InterruptedException | InvocationTargetException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createDialog() {
        lyricLabel = new JLabel("", SwingConstants.CENTER);
        lyricLabel.setFont(new Font("Dialog", Font.BOLD, FULL_LINE_FONT));
        lyricLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (latch != null) latch.countDown();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        content.add(lyricLabel,  BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog((Frame) null, "Nude", false);
        dialog.setContentPane(content);
        dialog.setMinimumSize(new Dimension(300, 120));
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void showLyric(String text, int fontSize) throws InterruptedException {
        latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            lyricLabel.setFont(new Font("Dialog", Font.BOLD, fontSize));
            lyricLabel.setText("<html><center>" + text + "</center></html>");
        });
        latch.await();
    }
}
