package com.lyricvideo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * RetroDialog — An undecorated JDialog hand-painted to look like a classic
 * Windows 95 / 98 / XP message box.
 *
 * <h3>Visual anatomy</h3>
 * <pre>
 * ┌────────────────────────────────────────────┐  ← outer raised 3-D border
 * │▓▓▓▓▓▓▓▓▓▓ Nude ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ [✕]│  ← blue gradient title bar
 * ├────────────────────────────────────────────┤
 * │  ⚠   Lyric text goes here                  │  ← silver content area
 * │                                            │     with warning icon
 * ├────────────────────────────────────────────┤
 * │              [   OK   ]                    │  ← silver button bar
 * └────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Usage</h3>
 * <pre>
 *   RetroDialog dlg = new RetroDialog(owner, "Nude", 400, 16);
 *   dlg.setLyricText("some lyric line");
 *   dlg.positionWithJitter(60);
 *   dlg.setVisible(true);
 * </pre>
 *
 * <p>Create a <em>new</em> instance for each lyric popup so the position
 * jitter is independent. Call {@link #dispose()} after hiding.
 */
public class RetroDialog extends JDialog {

    // ── Windows Classic colour palette (Win95 / XP "Silver" theme) ───────────
    private static final Color C_TITLE_L     = new Color(  0,   0, 128); // title bar left edge
    private static final Color C_TITLE_R     = new Color( 16, 132, 208); // title bar right edge
    private static final Color C_SILVER      = new Color(192, 192, 192); // window / button face
    private static final Color C_HIGHLIGHT   = Color.WHITE;              // 3-D bright edge
    private static final Color C_SHADOW      = new Color(128, 128, 128); // 3-D inner shadow
    private static final Color C_DARK_SHADOW = new Color( 64,  64,  64); // 3-D outer shadow
    private static final Color C_TITLE_TEXT  = Color.WHITE;
    private static final Color C_TEXT        = Color.BLACK;
    private static final Color C_WARN_YELLOW = new Color(255, 213,   0); // warning triangle fill

    // ── Layout constants (pixels) ─────────────────────────────────────────────
    private static final int TITLE_H   = 26; // title bar height
    private static final int BTN_BAR_H = 38; // bottom button bar height
    private static final int PADDING   = 10; // content area inner padding
    private static final int BORDER_PX =  2; // outer 3-D border thickness

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font F_TITLE  = new Font("Tahoma", Font.BOLD,  11);
    private static final Font F_BUTTON = new Font("Tahoma", Font.PLAIN, 11);

    private static final Random RAND = new Random();

    // ── State ─────────────────────────────────────────────────────────────────

    /** Label that holds the lyric text in the content area. */
    private final JLabel lyricLabel;

    /**
     * {@code true} once the user manually clicks [✕] or OK.
     * The sequencer can check this to cut a popup short if needed.
     */
    private volatile boolean dismissed = false;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param owner      The owning BackgroundFrame (keeps dialog on top of it)
     * @param title      Text shown in the blue title bar (e.g. "Nude")
     * @param width      Dialog width in pixels — use ~400 for full lines,
     *                   ~270 for single-word word-by-word popups
     * @param fontSize   Font size (pt) of the lyric text — use ~16 for full
     *                   lines, ~22 for single words (more dramatic)
     */
    public RetroDialog(Frame owner, String title, int width, int fontSize) {
        super(owner, false); // non-modal: sequencer thread can sleep while dialog is open
        setUndecorated(true);

        // Content area height scales with font so longer text is never clipped
        int contentH = Math.max(72, fontSize * 4);
        int totalH   = BORDER_PX * 2 + TITLE_H + contentH + BTN_BAR_H;
        setSize(width, totalH);

        // Create lyric label — HTML enables automatic word-wrap inside a fixed width
        lyricLabel = new JLabel("", SwingConstants.LEFT);
        lyricLabel.setFont(new Font("Tahoma", Font.PLAIN, fontSize));
        lyricLabel.setForeground(C_TEXT);
        lyricLabel.setVerticalAlignment(SwingConstants.CENTER);

        setContentPane(buildRootPanel(title));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Set the lyric text displayed in the content area.
     * Basic HTML is supported (e.g. {@code <b>word</b>} for emphasis).
     */
    public void setLyricText(String text) {
        // Wrap in HTML so JLabel word-wraps; width:100% lets it fill the panel
        lyricLabel.setText("<html><body style='width:100%;'>" + text + "</body></html>");
    }

    /**
     * Position the dialog near the screen centre with a random offset so
     * consecutive popups do not appear in exactly the same spot.
     *
     * @param jitter  Maximum pixel offset in each axis (e.g. 60)
     */
    public void positionWithJitter(int jitter) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int baseX = (screen.width  - getWidth())  / 2;
        int baseY = (screen.height - getHeight()) / 2;

        // Random offset — positive or negative, up to jitter pixels
        int dx = RAND.nextInt(jitter * 2 + 1) - jitter;
        int dy = RAND.nextInt(jitter * 2 + 1) - jitter;

        // Clamp to ensure the dialog never goes off-screen
        int x = Math.max(0, Math.min(screen.width  - getWidth(),  baseX + dx));
        int y = Math.max(0, Math.min(screen.height - getHeight(), baseY + dy));
        setLocation(x, y);
    }

    /** @return {@code true} if the user clicked [✕] or OK to dismiss this popup early. */
    public boolean isDismissed() { return dismissed; }

    // ── UI construction ───────────────────────────────────────────────────────

    /** Assembles the root panel: outer border → title bar + content + button bar. */
    private JPanel buildRootPanel(String title) {
        // Outer panel draws the classic raised 2-pixel window border
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw3DBorder(g, 0, 0, getWidth(), getHeight(), true, BORDER_PX);
            }
        };
        root.setBackground(C_SILVER);
        root.setBorder(BorderFactory.createEmptyBorder(BORDER_PX, BORDER_PX, BORDER_PX, BORDER_PX));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(C_SILVER);
        inner.add(buildTitleBar(title), BorderLayout.NORTH);
        inner.add(buildContentPanel(),  BorderLayout.CENTER);
        inner.add(buildButtonBar(),     BorderLayout.SOUTH);

        root.add(inner, BorderLayout.CENTER);
        return root;
    }

    /** Blue gradient title bar with title label and custom [✕] close button. */
    private JPanel buildTitleBar(String title) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Left-to-right gradient: dark navy → lighter cobalt
                g2.setPaint(new GradientPaint(0, 0, C_TITLE_L, getWidth(), 0, C_TITLE_R));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, TITLE_H));
        bar.setOpaque(false);

        // Title text (white, bold Tahoma)
        JLabel titleLabel = new JLabel("  " + title);
        titleLabel.setFont(F_TITLE);
        titleLabel.setForeground(C_TITLE_TEXT);
        bar.add(titleLabel, BorderLayout.CENTER);

        // [✕] close button
        JButton closeBtn = buildWin95Button("✕", 10);
        closeBtn.setPreferredSize(new Dimension(18, 18));
        closeBtn.addActionListener(e -> { dismissed = true; setVisible(false); });

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        eastPanel.setOpaque(false);
        eastPanel.add(closeBtn);
        bar.add(eastPanel, BorderLayout.EAST);

        return bar;
    }

    /** Silver content area: warning icon on the left, lyric text on the right. */
    private JPanel buildContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(PADDING, 0));
        panel.setBackground(C_SILVER);
        panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // 32×32 warning triangle — painted, no image asset required
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawWarningIcon((Graphics2D) g, 0, 0, 32, 32);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(42, 42);
            }
        };
        iconPanel.setBackground(C_SILVER);

        panel.add(iconPanel,  BorderLayout.WEST);
        panel.add(lyricLabel, BorderLayout.CENTER);
        return panel;
    }

    /** Silver button bar with a centred OK button. */
    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        bar.setBackground(C_SILVER);

        JButton okBtn = buildWin95Button("OK", 11);
        okBtn.setPreferredSize(new Dimension(75, 23));
        okBtn.addActionListener(e -> { dismissed = true; setVisible(false); });
        bar.add(okBtn);

        return bar;
    }

    // ── Win95 button factory ──────────────────────────────────────────────────

    /**
     * Creates a button with custom painting that mimics the Windows 95 raised
     * grey button style. The button has no LAF border; all edges are drawn by
     * {@link #drawRaisedRect}.
     *
     * @param label     Button text
     * @param fontSize  Font point size
     */
    private static JButton buildWin95Button(String label, int fontSize) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                drawRaisedRect(g, 0, 0, getWidth(), getHeight());

                // Centre the label text inside the button
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g.setColor(C_TEXT);
                g.drawString(getText(), tx, ty);
            }
        };
        btn.setFont(new Font("Tahoma", Font.PLAIN, fontSize));
        btn.setBorder(null);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false); // let paintComponent handle the background
        btn.setCursor(Cursor.getDefaultCursor());
        return btn;
    }

    // ── Drawing primitives ────────────────────────────────────────────────────

    /**
     * Fills a rectangle with the silver button colour, then paints a Win95
     * raised 3-D bevel around its edges.
     *
     * <pre>
     *   White top+left  →  looks "raised"
     *   Gray/dark bottom+right
     * </pre>
     */
    private static void drawRaisedRect(Graphics g, int x, int y, int w, int h) {
        // Fill
        g.setColor(C_SILVER);
        g.fillRect(x, y, w, h);

        // Outer bright edges (top, left)
        g.setColor(C_HIGHLIGHT);
        g.drawLine(x,     y,     x+w-2, y    ); // top
        g.drawLine(x,     y,     x,     y+h-2); // left

        // Outer dark shadow edges (bottom, right)
        g.setColor(C_DARK_SHADOW);
        g.drawLine(x+w-1, y,     x+w-1, y+h-1); // right outer
        g.drawLine(x,     y+h-1, x+w-1, y+h-1); // bottom outer

        // Inner medium shadow (one pixel inside the dark shadow)
        g.setColor(C_SHADOW);
        g.drawLine(x+w-2, y+1,   x+w-2, y+h-2); // right inner
        g.drawLine(x+1,   y+h-2, x+w-2, y+h-2); // bottom inner
    }

    /**
     * Draws a raised or sunken 3-D border of {@code thickness} pixel-pairs
     * around a rectangle. Mimics the Win32 {@code DrawEdge} API.
     *
     * @param raised     {@code true} → bright top-left / dark bottom-right (raised);
     *                   {@code false} → inverted (sunken / pressed)
     * @param thickness  Number of highlight+shadow line pairs to draw
     */
    private static void draw3DBorder(Graphics g,
                                     int x, int y, int w, int h,
                                     boolean raised, int thickness) {
        for (int i = 0; i < thickness; i++) {
            // Top and left edges
            g.setColor(raised ? C_HIGHLIGHT : C_DARK_SHADOW);
            g.drawLine(x+i, y+i, x+w-2-i, y+i  );  // top
            g.drawLine(x+i, y+i, x+i,     y+h-2-i); // left

            // Bottom and right edges
            g.setColor(raised ? C_DARK_SHADOW : C_HIGHLIGHT);
            g.drawLine(x+w-1-i, y+i,     x+w-1-i, y+h-1-i); // right
            g.drawLine(x+i,     y+h-1-i, x+w-1-i, y+h-1-i); // bottom
        }
    }

    /**
     * Draws a classic Windows warning icon: a yellow equilateral triangle with
     * a bold "!" inside. Rendered with anti-aliasing so it looks clean at any
     * dialog size.
     *
     * @param g2  Graphics2D context of the icon panel
     * @param x   Panel-relative X of the icon bounding box
     * @param y   Panel-relative Y of the icon bounding box
     * @param w   Icon width
     * @param h   Icon height
     */
    private static void drawWarningIcon(Graphics2D g2,
                                        int x, int y, int w, int h) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // Triangle vertices: apex at top-centre, base along the bottom
        int[] px = { x + w / 2,  x + 1,      x + w - 1  };
        int[] py = { y + 1,      y + h - 1,  y + h - 1  };

        g2.setColor(C_WARN_YELLOW);
        g2.fillPolygon(px, py, 3);

        g2.setColor(C_DARK_SHADOW);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(px, py, 3);

        // Bold exclamation mark centred in the lower two-thirds of the triangle
        g2.setFont(new Font("Arial", Font.BOLD, h / 2));
        FontMetrics fm = g2.getFontMetrics();
        String bang = "!";
        int tx = x + (w - fm.stringWidth(bang)) / 2;
        int ty = y + h - h / 7;  // push toward base of triangle
        g2.setColor(C_DARK_SHADOW);
        g2.drawString(bang, tx, ty);

        g2.dispose();
    }
}
