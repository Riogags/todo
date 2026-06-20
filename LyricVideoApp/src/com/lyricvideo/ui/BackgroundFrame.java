package com.lyricvideo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * BackgroundFrame — A dark, undecorated window that fills the screen to serve
 * as the "desktop" background visible between and behind lyric popups.
 *
 * <h3>Full-screen vs. windowed</h3>
 * Set {@link #FULLSCREEN_MODE} to {@code true} for the final recording.
 * During development, keep it {@code false} so your IDE remains usable.
 *
 * <h3>Keyboard shortcuts</h3>
 * <ul>
 *   <li><b>ESC</b> — quit immediately</li>
 * </ul>
 */
public class BackgroundFrame extends JFrame {

    /**
     * Toggle between full-screen and windowed mode.
     *
     * <ul>
     *   <li>{@code true}  → exclusive full-screen (best for recording)</li>
     *   <li>{@code false} → 1280×720 window (easier for development/preview)</li>
     * </ul>
     */
    public static final boolean FULLSCREEN_MODE = true;

    // ── Gradient colours for the desktop background ───────────────────────────
    // These give a deep space / void feel that makes the silver popups pop.
    private static final Color BG_TOP    = new Color(10,  10, 20);
    private static final Color BG_BOTTOM = new Color(22,  18, 34);

    // ─────────────────────────────────────────────────────────────────────────

    public BackgroundFrame() {
        setTitle("Lyric Video — press ESC to stop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);

        JPanel canvas = buildCanvas();
        setContentPane(canvas);

        bindEscapeKey(canvas);
        applyDisplayMode();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Creates the dark gradient panel that acts as the desktop surface. */
    private JPanel buildCanvas() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);

                // Vertical gradient from near-black at the top to deep purple-black at bottom
                g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle horizontal scan-line texture (every 4px) for a CRT monitor feel
                g2.setColor(new Color(0, 0, 0, 18));
                for (int y = 0; y < getHeight(); y += 4) {
                    g2.drawLine(0, y, getWidth(), y);
                }
            }
        };
    }

    /** Binds the ESC key to a clean shutdown. */
    private void bindEscapeKey(JComponent target) {
        target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "quit");
        target.getActionMap().put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    /** Applies full-screen exclusive mode or falls back to a maximised window. */
    private void applyDisplayMode() {
        if (FULLSCREEN_MODE) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(this); // exclusive full-screen
            } else {
                // OS doesn't support exclusive mode — fill screen manually
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                setSize(screen);
                setLocation(0, 0);
                setVisible(true);
            }
        } else {
            // Development / preview mode: 1280×720 centred window
            setSize(1280, 720);
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }
}
