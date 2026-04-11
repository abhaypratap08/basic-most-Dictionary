import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class Dickt extends JFrame {
    // ── Palette ─────────────────────────────────────────────
    static final Color BG = new Color(0x1C1B1F);
    static final Color SURFACE = new Color(0x2B2930);
    static final Color SURFACE_HI = new Color(0x3A3741);
    static final Color PRIMARY = new Color(0xD0BCFF);
    static final Color ON_PRI = new Color(0x38006B);
    static final Color ON_SURF = new Color(0xE6E1E5);
    static final Color ON_SURF2 = new Color(0xCAC4D0);
    static final Color MUTED = new Color(0x938F99);

    static final int W = 360;
    static final int PAD = 16;
    static final int RAD = 28;

    private JTextField searchInput;
    private JLabel statusLabel, wordLabel;
    private JTextArea meaningArea, exampleArea;
    private JPanel resultCard, root;
    private Point dragOrigin;
    private boolean showingResult = false;
    private boolean customWindowMaskSupported = false;

    public Dickt() {
        setUndecorated(true);
        configureWindowEffects();
        setAlwaysOnTop(true);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Root
        root = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RAD, RAD);
                g2.dispose();
            }
        };
        setContentPane(root);

        // Drag + right-click close
        MouseAdapter drag = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) System.exit(0);
                dragOrigin = e.getPoint();
            }
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - dragOrigin.x,
                            p.y + e.getY() - dragOrigin.y);
            }
        };
        root.addMouseListener(drag);
        root.addMouseMotionListener(drag);

        // Logo
        JLabel logo = new JLabel("dickt...");
        logo.setFont(new Font("SansSerif", Font.BOLD, 18));
        logo.setForeground(PRIMARY);

        // Close
        JButton close = new JButton("✕");
        close.setForeground(MUTED);
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setFocusPainted(false);
        close.addActionListener(e -> System.exit(0));

        // Search pill
        JPanel pill = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE_HI);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 50, 50);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(10, 16, 10, 10));

        JLabel icon = new JLabel("🔍 ");
        icon.setForeground(MUTED);

        searchInput = new JTextField();
        searchInput.setBorder(null);
        searchInput.setOpaque(false);
        searchInput.setForeground(ON_SURF);
        searchInput.setFont(new Font("SansSerif", Font.PLAIN, 15));
        searchInput.addActionListener(e -> handleSearch());

        JButton go = new JButton("Go") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? PRIMARY.darker() : PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 50, 50);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        go.setForeground(ON_PRI);
        go.setFont(new Font("SansSerif", Font.BOLD, 12));
        go.setContentAreaFilled(false);
        go.setBorderPainted(false);
        go.setFocusPainted(false);
        go.setBorder(new EmptyBorder(6, 16, 6, 16));
        go.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        go.addActionListener(e -> handleSearch());

        pill.add(icon, BorderLayout.WEST);
        pill.add(searchInput, BorderLayout.CENTER);
        pill.add(go, BorderLayout.EAST);

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);

        // Result card
        resultCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 2, 20, 20);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));
        resultCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        wordLabel = new JLabel("WORD");
        wordLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        wordLabel.setForeground(PRIMARY);

        meaningArea = buildTextArea("Meaning");
        exampleArea = buildTextArea("Example");

        resultCard.add(wordLabel);
        resultCard.add(Box.createRigidArea(new Dimension(0, 8)));
        resultCard.add(meaningArea);
        resultCard.add(Box.createRigidArea(new Dimension(0, 8)));
        resultCard.add(exampleArea);

        // Add everything to root
        root.add(logo);
        root.add(close);
        root.add(pill);
        root.add(statusLabel);
        root.add(resultCard);

        setSize(W, compactHeight());
        doLayout(root);
        setVisible(true);
        reShape();
        setLocationRelativeTo(null);
    }

    private void doLayout(JPanel root) {
        int iw = W - PAD * 2;
        int y = 0;

        root.getComponent(0).setBounds(PAD, 14, 100, 22);   // logo
        root.getComponent(1).setBounds(W - PAD - 30, 12, 30, 24); // close

        y = 52;
        root.getComponent(2).setBounds(PAD, y, iw, 42);     // pill
        y += 54;

        root.getComponent(3).setBounds(PAD, y, iw, 18);     // status
        y += 26;

        Component card = root.getComponent(4);
        if (showingResult) {
            card.setSize(iw, 1);
            int h = card.getPreferredSize().height;
            card.setBounds(PAD, y, iw, h);
            y += h + PAD;
        } else {
            card.setBounds(PAD, y, iw, 0);
            y += PAD;
        }

        setSize(W, y);
        reShape();
        root.repaint();
    }

    private int compactHeight() {
        return 52 + 42 + 12 + 18 + 8 + PAD;
    }

    private void reShape() {
        if (!customWindowMaskSupported) {
            return;
        }
        try {
            setShape(new RoundRectangle2D.Float(0, 0, W, getHeight(), RAD, RAD));
        } catch (UnsupportedOperationException ignored) {
            customWindowMaskSupported = false;
        }
    }

    private void configureWindowEffects() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            try {
                setBackground(new Color(0, 0, 0, 0));
                customWindowMaskSupported = true;
                return;
            } catch (UnsupportedOperationException ignored) {
                // Fall back to an opaque window when the platform rejects translucency at runtime.
            }
        }

        setBackground(BG);
    }

    private void handleSearch() {
        String word = searchInput.getText().trim();
        if (word.isEmpty()) return;

        statusLabel.setText("Looking up \"" + word + "\"...");
        showingResult = false;
        doLayout(root);
        fetch(word);
    }

    private void fetch(String word) {
        String encoded = URLEncoder.encode(word, StandardCharsets.UTF_8);
        String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + encoded;

        HttpClient.newHttpClient()
                .sendAsync(
                        HttpRequest.newBuilder().uri(URI.create(url)).build(),
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenAccept(res -> {
                    if (res.statusCode() != 200) {
                        SwingUtilities.invokeLater(() ->
                                statusLabel.setText("HTTP " + res.statusCode()));
                        return;
                    }
                    process(res.body());
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText("Network error"));
                    return null;
                });
    }

    private void process(String json) {
        SwingUtilities.invokeLater(() -> {
            try {
                JSONArray arr = new JSONArray(json);
                JSONObject obj = arr.getJSONObject(0);

                String word = obj.getString("word");

                JSONObject def = obj.getJSONArray("meanings")
                        .getJSONObject(0)
                        .getJSONArray("definitions")
                        .getJSONObject(0);

                wordLabel.setText(word.toUpperCase());
                meaningArea.setText(def.getString("definition"));

                String ex = def.optString("example", "");
                exampleArea.setText(ex.isEmpty() ? "No example available." : ex);

                statusLabel.setText(" ");
                showingResult = true;
            } catch (Exception e) {
                statusLabel.setText("No definition found");
                showingResult = false;
            }
            doLayout(root);
        });
    }

    private JTextArea buildTextArea(String title) {
        JTextArea a = new JTextArea();
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setOpaque(false);
        a.setForeground(ON_SURF2);
        a.setFont(new Font("SansSerif", Font.PLAIN, 13));
        a.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 11),
                MUTED
        ));
        return a;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Dickt::new);
    }
}
