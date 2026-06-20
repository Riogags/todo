package com.lyricvideo;

import com.lyricvideo.config.LyricConfig;
import com.lyricvideo.ui.LyricSequencer;

import javax.swing.SwingUtilities;

/**
 * Entry point. Starts the lyric popup sequence — click OK on each popup to advance.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LyricSequencer sequencer = new LyricSequencer(LyricConfig.getLyrics());
            sequencer.start();
        });
    }
}
