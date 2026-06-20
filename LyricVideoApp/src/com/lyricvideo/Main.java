package com.lyricvideo;

import com.lyricvideo.config.LyricConfig;
import com.lyricvideo.ui.BackgroundFrame;
import com.lyricvideo.ui.LyricSequencer;

import javax.swing.SwingUtilities;

/**
 * Application entry point for the Windows-style lyric video generator.
 *
 * <p>This program displays a sequence of retro Windows 95/XP-style popup
 * dialogs, one per lyric entry, intended to be <strong>screen-recorded</strong>.
 * No audio is played — add the music track in your video editor afterwards.
 *
 * <h2>How to run</h2>
 * <ol>
 *   <li>Open IntelliJ IDEA → File → Open → select the {@code LyricVideoApp} folder.</li>
 *   <li>Configure an SDK: File → Project Structure → SDKs → add JDK 11 or later.</li>
 *   <li>Click the green Run button next to {@code main()} or press Shift+F10.</li>
 *   <li>A dark full-screen window appears; lyric popups follow automatically.</li>
 *   <li>Press <b>ESC</b> at any time to exit.</li>
 * </ol>
 *
 * <h2>To customize lyrics and timing</h2>
 * Edit {@code src/com/lyricvideo/config/LyricConfig.java}. Every entry is
 * annotated with clear instructions for what text and timing values to insert.
 *
 * <h2>Screen-recording tips</h2>
 * <ul>
 *   <li>OBS Studio (free): add a Display Capture source, set the canvas to
 *       your target resolution (e.g. 1920×1080), and hit Record.</li>
 *   <li>Set {@link com.lyricvideo.ui.BackgroundFrame#FULLSCREEN_MODE} to
 *       {@code false} while adjusting timings so the IDE stays accessible,
 *       then flip it back to {@code true} before the final recording.</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {
        // Bootstrap on the Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // 1. Dark full-screen background (the "desktop")
            BackgroundFrame background = new BackgroundFrame();

            // 2. Sequencer reads LyricConfig and drives popup timing
            //    on a background daemon thread — returns immediately here
            LyricSequencer sequencer = new LyricSequencer(background, LyricConfig.getLyrics());
            sequencer.start();
        });
    }
}
