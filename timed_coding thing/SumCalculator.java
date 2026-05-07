import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * SumCalculator — A polished multi-operation calculator GUI.
 *
 *   Screens: Welcome  →  Menu  →  Calculator  →  Goodbye
 *
 *   Features
 *     • Resizable / maximizable window (works windowed and full-size)
 *     • Ctrl + and Ctrl - to zoom UI in / out (Ctrl 0 to reset)
 *     • F11 toggles full-screen
 *     • Menu of operations: Sum, Subtract, Multiply, Divide, Average,
 *       Power, Square Root, Modulus, Min, Max
 *     • Each operation builds its own input boxes that match the formula
 *
 *   Compile:  javac SumCalculator.java
 *   Run:      java SumCalculator
 */
public class SumCalculator extends JFrame {

    // ── Palette (manga: cream paper, black ink, splash red) ───────────────────
    private static final Color BG         = new Color(245, 241, 232);  // cream paper
    private static final Color BG2        = new Color(0,   0,   0, 36); // halftone dots
    private static final Color CARD       = new Color(255, 255, 255);  // white panel
    private static final Color ACCENT     = new Color(0,   0,   0);    // pure black
    private static final Color ACCENT2    = new Color(60,  60,  60);   // dark gray
    private static final Color TEXT       = new Color(15,  15,  15);   // near-black
    private static final Color MUTED      = new Color(120, 120, 120);  // mid gray
    private static final Color SUCCESS    = new Color(15,  15,  15);   // black (no neon green)
    private static final Color DANGER     = new Color(200, 20,  20);   // splash red
    private static final Color FIELD_BG   = new Color(250, 248, 242);  // very light cream
    private static final Color FIELD_BD   = new Color(0,   0,   0);    // black border
    private static final Color INK        = new Color(0,   0,   0);    // pure ink
    private static final Color BURST_FILL = new Color(255, 255, 255);  // white burst
    private static final Color BURST_TEXT = new Color(0,   0,   0);    // black "BAM!"
    private static final Color HOVER_LIGHT = new Color(225, 222, 213); // hover on cream surfaces
    private static final Color PRESS_LIGHT = new Color(195, 192, 184); // press on cream surfaces
    private static final Color HOVER_DARK  = new Color(50,  50,  50);  // hover on black surfaces
    private static final Color PRESS_DARK  = new Color(35,  35,  35);  // press on black surfaces

    // ── Comic-book font (with fallbacks) ───────────────────────────────────────
    private static final String COMIC_FONT = pickComicFont();
    private static String pickComicFont() {
        String[] candidates = {
            "Comic Sans MS", "Comic Neue", "Chalkboard SE", "Chalkboard",
            "Marker Felt", "Bradley Hand", "Comic Sans"
        };
        java.util.Set<String> available = new java.util.HashSet<>(
            java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                                    .getAvailableFontFamilyNames()));
        for (String c : candidates) if (available.contains(c)) return c;
        return "SansSerif"; // sturdy fallback if no comic font is installed
    }

    // ── State ──────────────────────────────────────────────────────────────────
    private String userName = "";
    private double lastResult = 0;
    private String lastOpName = "";
    private float fontScale = 1.0f;
    private boolean fullscreen = false;
    private Rectangle savedBounds;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    // ── Panels ─────────────────────────────────────────────────────────────────
    private JPanel welcomePanel;
    private JPanel menuPanel;
    private JPanel calcPanel;
    private JPanel thankPanel;

    private JPanel calcContainer;       // holds the dynamic calc card
    private JTextField nameField;
    private JLabel thankNameLabel;
    private JLabel thankSumLabel;
    private final ComicBurst burst = new ComicBurst();

    // Track every component that has a font we want to scale
    private final List<ScaledFont> scaledFonts = new ArrayList<>();

    // ── Operations ─────────────────────────────────────────────────────────────
    static class Operation {
        final String name, symbol, formula, description;
        final String[] inputLabels;
        final Function<double[], Double> compute;
        Operation(String name, String symbol, String formula, String description,
                  String[] inputLabels, Function<double[], Double> compute) {
            this.name = name;
            this.symbol = symbol;
            this.formula = formula;
            this.description = description;
            this.inputLabels = inputLabels;
            this.compute = compute;
        }
    }

    private final Operation[] operations = {
        new Operation("Sum", "∑", "a + b + c + d",
            "Adds all the numbers together to get one big total.",
            new String[]{"a", "b", "c", "d"},
            v -> v[0] + v[1] + v[2] + v[3]),
        new Operation("Subtract", "−", "a − b",
            "Takes the second number away from the first.",
            new String[]{"a", "b"},
            v -> v[0] - v[1]),
        new Operation("Multiply", "×", "a × b",
            "Adds 'a' to itself 'b' times — repeated addition, fast!",
            new String[]{"a", "b"},
            v -> v[0] * v[1]),
        new Operation("Divide", "÷", "a ÷ b",
            "Splits 'a' into 'b' equal pieces. (No dividing by zero!)",
            new String[]{"a (dividend)", "b (divisor)"},
            v -> {
                if (v[1] == 0) throw new ArithmeticException("Cannot divide by zero");
                return v[0] / v[1];
            }),
        new Operation("Average", "x̄", "(a + b + c + d) ÷ 4",
            "Adds the numbers, then divides by how many there are.",
            new String[]{"a", "b", "c", "d"},
            v -> (v[0] + v[1] + v[2] + v[3]) / 4.0),
        new Operation("Power", "^", "a ^ b",
            "Multiplies 'a' by itself 'b' times. 2^3 = 2·2·2 = 8.",
            new String[]{"a (base)", "b (exponent)"},
            v -> Math.pow(v[0], v[1])),
        new Operation("Square Root", "√", "√a",
            "Finds the number that, times itself, equals 'a'.",
            new String[]{"a"},
            v -> {
                if (v[0] < 0) throw new ArithmeticException("Cannot take √ of a negative");
                return Math.sqrt(v[0]);
            }),
        new Operation("Modulus", "%", "a mod b",
            "The leftover after dividing 'a' by 'b'. 7 mod 3 = 1.",
            new String[]{"a", "b"},
            v -> {
                if (v[1] == 0) throw new ArithmeticException("Cannot mod by zero");
                return v[0] % v[1];
            }),
        new Operation("Minimum", "↓", "min(a, b, c)",
            "Picks the smallest of the three numbers.",
            new String[]{"a", "b", "c"},
            v -> Math.min(v[0], Math.min(v[1], v[2]))),
        new Operation("Maximum", "↑", "max(a, b, c)",
            "Picks the largest of the three numbers.",
            new String[]{"a", "b", "c"},
            v -> Math.max(v[0], Math.max(v[1], v[2]))),
    };

    // ══════════════════════════════════════════════════════════════════════════
    public SumCalculator() {
        super("Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 720);
        setMinimumSize(new Dimension(560, 560));
        setLocationRelativeTo(null);
        setResizable(true);

        root.setBackground(BG);
        buildWelcomePanel();
        buildMenuPanel();
        buildThankPanel();
        calcContainer = new GradientPanel(BG, BG2);
        calcContainer.setLayout(new GridBagLayout());

        root.add(welcomePanel,  "WELCOME");
        root.add(menuPanel,     "MENU");
        root.add(calcContainer, "CALC");
        root.add(thankPanel,    "THANK");

        add(root);
        setGlassPane(burst);
        burst.setVisible(false);
        cardLayout.show(root, "WELCOME");
        installKeybindings();
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  KEYBINDINGS  (Ctrl + / Ctrl - / Ctrl 0  / F11)
    // ══════════════════════════════════════════════════════════════════════════
    private void installKeybindings() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,    ctrl), "zoomIn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,      ctrl), "zoomIn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,       ctrl), "zoomIn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,     ctrl), "zoomOut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,  ctrl), "zoomOut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0,         ctrl), "zoomReset");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0,   ctrl), "zoomReset");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),          "fullscreen");

        am.put("zoomIn",     act(e -> setScale(fontScale + 0.1f)));
        am.put("zoomOut",    act(e -> setScale(fontScale - 0.1f)));
        am.put("zoomReset",  act(e -> setScale(1.0f)));
        am.put("fullscreen", act(e -> toggleFullscreen()));
    }

    private Action act(java.util.function.Consumer<ActionEvent> c) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { c.accept(e); }
        };
    }

    private void setScale(float s) {
        s = Math.max(0.6f, Math.min(2.5f, s));
        fontScale = s;
        for (ScaledFont sf : scaledFonts) sf.apply(fontScale);
        revalidate();
        repaint();
    }

    private void toggleFullscreen() {
        if (!fullscreen) {
            savedBounds = getBounds();
            dispose();
            setUndecorated(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
            fullscreen = true;
        } else {
            dispose();
            setUndecorated(false);
            setExtendedState(JFrame.NORMAL);
            if (savedBounds != null) setBounds(savedBounds);
            setVisible(true);
            fullscreen = false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCREEN 1 – WELCOME
    // ══════════════════════════════════════════════════════════════════════════
    private void buildWelcomePanel() {
        welcomePanel = new GradientPanel(BG, BG2);
        welcomePanel.setLayout(new GridBagLayout());

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(46, 46, 46, 46));

        JLabel icon = makeLabel("✦", 56, ACCENT);
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = makeLabel("Welcome!", 34, TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        registerScaled(title, 34, Font.BOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = makeLabel("A friendly little calculator. What's your name?",
                14, MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JLabel hint = makeLabel(
            "Tip: Ctrl + / Ctrl − to zoom, F11 for full-screen.",
            11, MUTED);
        hint.setAlignmentX(CENTER_ALIGNMENT);

        JLabel nameLbl = makeLabel("Your Name", 13, ACCENT);
        nameLbl.setAlignmentX(LEFT_ALIGNMENT);

        nameField = makeTextField("e.g. Alex");
        nameField.setAlignmentX(LEFT_ALIGNMENT);
        nameField.addActionListener(e -> goToMenu());

        JButton btn = makePrimaryButton("Continue →");
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.addActionListener(e -> goToMenu());

        card.add(icon);
        card.add(vgap(10));
        card.add(title);
        card.add(vgap(6));
        card.add(sub);
        card.add(vgap(2));
        card.add(hint);
        card.add(vgap(28));
        card.add(nameLbl);
        card.add(vgap(6));
        card.add(nameField);
        card.add(vgap(26));
        card.add(btn);

        addCenteredCard(welcomePanel, card);
    }

    private void goToMenu() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            shake(nameField);
            flashError(nameField);
            return;
        }
        userName = name;
        cardLayout.show(root, "MENU");
        rebuildMenuGreeting();
        burst.popHere("WOW!");
    }

    private JLabel menuGreeting;
    private void rebuildMenuGreeting() {
        if (menuGreeting != null)
            menuGreeting.setText("Hi, " + userName + " — pick what you'd like to calculate:");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCREEN 2 – MENU
    // ══════════════════════════════════════════════════════════════════════════
    private void buildMenuPanel() {
        menuPanel = new GradientPanel(BG, BG2);
        menuPanel.setLayout(new GridBagLayout());

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 42, 36, 42));

        JLabel icon = makeLabel("≡", 42, ACCENT2);
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = makeLabel("Choose an Operation", 24, TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        registerScaled(title, 24, Font.BOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);

        menuGreeting = makeLabel("Pick what you'd like to calculate:", 13, MUTED);
        menuGreeting.setAlignmentX(CENTER_ALIGNMENT);

        card.add(icon);
        card.add(vgap(6));
        card.add(title);
        card.add(vgap(4));
        card.add(menuGreeting);
        card.add(vgap(22));

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        for (Operation op : operations) {
            grid.add(makeOpButton(op));
        }
        card.add(grid);
        card.add(vgap(20));

        JButton back = makeSecondaryButton("← Back");
        back.setAlignmentX(CENTER_ALIGNMENT);
        back.addActionListener(e -> cardLayout.show(root, "WELCOME"));

        JButton quit = makeSecondaryButton("Exit");
        quit.setAlignmentX(CENTER_ALIGNMENT);
        quit.addActionListener(e -> goToThankWithoutResult());

        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.add(back);
        row.add(quit);
        card.add(row);

        addCenteredCard(menuPanel, card);
    }

    private JButton makeOpButton(Operation op) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = FIELD_BG;
                if (getModel().isPressed())       base = PRESS_LIGHT;
                else if (getModel().isRollover()) base = HOVER_LIGHT;
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(getModel().isRollover() ? 3f : 2f,
                                             BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setLayout(new BorderLayout());
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(220, 96));
        b.setToolTipText(op.description);

        JLabel sym = makeLabel(op.symbol, 30, ACCENT);
        sym.setBorder(new EmptyBorder(0, 14, 0, 8));
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setBorder(new EmptyBorder(8, 4, 8, 10));
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        JLabel name = makeLabel(op.name, 16, TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        registerScaled(name, 16, Font.BOLD);
        JLabel formula = makeLabel(op.formula, 12, ACCENT2);
        JLabel desc = makeLabel("<html><body style='width:160px'>" + op.description + "</body></html>",
                                11, MUTED);
        txt.add(name);
        txt.add(formula);
        txt.add(Box.createRigidArea(new Dimension(0, 2)));
        txt.add(desc);
        b.add(sym, BorderLayout.WEST);
        b.add(txt, BorderLayout.CENTER);
        b.addActionListener(e -> {
            Point p = burst.isShowing()
                ? SwingUtilities.convertPoint(b, b.getWidth()/2, b.getHeight()/2, burst)
                : new Point(getWidth()/2, getHeight()/2);
            burst.mangaPanelZoom(p, () -> openCalc(op));
        });
        return b;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCREEN 3 – CALCULATOR (built per operation)
    // ══════════════════════════════════════════════════════════════════════════
    private void openCalc(Operation op) {
        calcContainer.removeAll();
        calcContainer.add(buildCalcCard(op), centeredCardConstraints());
        cardLayout.show(root, "CALC");
        calcContainer.revalidate();
        calcContainer.repaint();
        // Reapply current font scale to the freshly-built panel
        setScale(fontScale);
    }

    private JPanel buildCalcCard(Operation op) {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 42, 36, 42));

        JLabel icon = makeLabel(op.symbol, 44, ACCENT2);
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = makeLabel(op.name, 24, TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        registerScaled(title, 24, Font.BOLD);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel formula = makeLabel(op.formula, 16, ACCENT);
        formula.setFont(formula.getFont().deriveFont(Font.BOLD));
        registerScaled(formula, 16, Font.BOLD);
        formula.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = makeLabel(
            "<html><body style='text-align:center; width:360px'>"
                + op.description + "</body></html>",
            13, TEXT);
        desc.setHorizontalAlignment(SwingConstants.CENTER);
        SpeechBubble bubble = new SpeechBubble(desc);
        bubble.setAlignmentX(CENTER_ALIGNMENT);
        bubble.setMaximumSize(new Dimension(440, 110));

        JLabel hello = makeLabel("Hi " + userName + " — fill in the values:", 12, MUTED);
        hello.setAlignmentX(CENTER_ALIGNMENT);

        card.add(icon);
        card.add(vgap(4));
        card.add(title);
        card.add(vgap(6));
        card.add(formula);
        card.add(vgap(10));
        card.add(bubble);
        card.add(vgap(2));
        card.add(hello);
        card.add(vgap(20));

        // Build input grid that matches the formula
        int n = op.inputLabels.length;
        int cols = Math.min(2, n);
        JPanel grid = new JPanel(new GridLayout(0, cols, 14, 14));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);

        JTextField[] fields = new JTextField[n];
        for (int i = 0; i < n; i++) {
            JPanel cell = new JPanel(new BorderLayout(0, 6));
            cell.setOpaque(false);
            JLabel lbl = makeLabel(op.inputLabels[i], 12, ACCENT);
            JTextField f = makeTextField("0");
            f.setHorizontalAlignment(JTextField.CENTER);
            fields[i] = f;
            final int idx = i;
            f.addActionListener(e -> {
                if (idx < fields.length - 1) fields[idx + 1].requestFocusInWindow();
            });
            cell.add(lbl, BorderLayout.NORTH);
            cell.add(f,   BorderLayout.CENTER);
            grid.add(cell);
        }
        card.add(grid);
        card.add(vgap(22));

        JLabel result = makeLabel("Result will appear here", 18, MUTED);
        result.setHorizontalAlignment(SwingConstants.CENTER);
        SpeechBubble resultBubble = new SpeechBubble(result);
        resultBubble.setAlignmentX(CENTER_ALIGNMENT);
        resultBubble.setMaximumSize(new Dimension(460, 90));
        card.add(resultBubble);
        card.add(vgap(18));

        JButton calcBtn  = makePrimaryButton("Calculate");
        JButton clearBtn = makeSecondaryButton("Clear");
        calcBtn.addActionListener(e -> doCompute(op, fields, result, resultBubble));
        clearBtn.addActionListener(e -> {
            for (JTextField f : fields) {
                f.setText("");
                resetFieldBorder(f);
            }
            result.setForeground(MUTED);
            result.setText("Result will appear here");
            fields[0].requestFocusInWindow();
        });

        // Enter on the last field calculates
        fields[fields.length - 1].addActionListener(
            e -> doCompute(op, fields, result, resultBubble));

        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(calcBtn);
        row.add(clearBtn);
        card.add(row);
        card.add(vgap(12));

        JButton menuBtn = makeSecondaryButton("← Other operations");
        menuBtn.setAlignmentX(CENTER_ALIGNMENT);
        menuBtn.addActionListener(e -> cardLayout.show(root, "MENU"));

        JButton doneBtn = makePrimaryButton("Done  →");
        doneBtn.setBackground(SUCCESS.darker());
        doneBtn.setAlignmentX(CENTER_ALIGNMENT);
        doneBtn.addActionListener(e -> {
            // Compute silently if possible, then go to thank-you
            try {
                lastResult = op.compute.apply(parseValues(fields));
                lastOpName = op.name;
            } catch (Exception ignored) { /* keep prior */ }
            goToThank(op);
        });

        JPanel finalRow = new JPanel(new GridLayout(1, 2, 14, 0));
        finalRow.setOpaque(false);
        finalRow.setAlignmentX(LEFT_ALIGNMENT);
        finalRow.add(menuBtn);
        finalRow.add(doneBtn);
        card.add(finalRow);

        return card;
    }

    private double[] parseValues(JTextField[] fields) {
        double[] v = new double[fields.length];
        for (int i = 0; i < fields.length; i++) {
            v[i] = Double.parseDouble(fields[i].getText().trim());
        }
        return v;
    }

    private void doCompute(Operation op, JTextField[] fields,
                           JLabel result, JComponent burstTarget) {
        boolean parseError = false;
        for (JTextField f : fields) {
            try {
                Double.parseDouble(f.getText().trim());
                resetFieldBorder(f);
            } catch (NumberFormatException ex) {
                parseError = true;
                errorFieldBorder(f);
                shake(f);
            }
        }
        if (parseError) {
            result.setForeground(DANGER);
            result.setText("⚠  Please enter valid numbers in every box.");
            return;
        }
        try {
            double answer = op.compute.apply(parseValues(fields));
            lastResult = answer;
            lastOpName = op.name;
            result.setForeground(SUCCESS);
            result.setText(op.name + "  =  " + format(answer));
            burst.popOver(burstTarget);
        } catch (ArithmeticException ex) {
            result.setForeground(DANGER);
            result.setText("⚠  " + ex.getMessage());
        }
    }

    private static String format(double d) {
        if (Double.isNaN(d))      return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "∞" : "−∞";
        if (d == Math.floor(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        String s = String.format("%.6f", d);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private void goToThank(Operation op) {
        thankNameLabel.setText("Thank you, " + userName + "!");
        thankSumLabel.setText(op.name + "  =  " + format(lastResult));
        cardLayout.show(root, "THANK");
        burst.popHere("BYE!");
    }

    private void goToThankWithoutResult() {
        thankNameLabel.setText("Goodbye" + (userName.isEmpty() ? "!" : ", " + userName + "!"));
        thankSumLabel.setText(lastOpName.isEmpty()
                ? "Come back anytime."
                : "Last result — " + lastOpName + " = " + format(lastResult));
        cardLayout.show(root, "THANK");
        burst.popHere("BYE!");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCREEN 4 – GOODBYE
    // ══════════════════════════════════════════════════════════════════════════
    private void buildThankPanel() {
        thankPanel = new GradientPanel(BG, BG2);
        thankPanel.setLayout(new GridBagLayout());

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(52, 42, 52, 42));

        JLabel icon = makeLabel("完", 64, INK);
        icon.setAlignmentX(CENTER_ALIGNMENT);

        thankNameLabel = makeLabel("Thank you!", 30, TEXT);
        thankNameLabel.setFont(thankNameLabel.getFont().deriveFont(Font.BOLD));
        registerScaled(thankNameLabel, 30, Font.BOLD);
        thankNameLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel msg = makeLabel("Here's your final result:", 14, MUTED);
        msg.setAlignmentX(CENTER_ALIGNMENT);

        thankSumLabel = makeLabel("", 26, SUCCESS);
        thankSumLabel.setFont(thankSumLabel.getFont().deriveFont(Font.BOLD));
        registerScaled(thankSumLabel, 26, Font.BOLD);
        thankSumLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel bye = makeLabel("Hope this helped — have a wonderful day!", 13, MUTED);
        bye.setAlignmentX(CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(FIELD_BD);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JButton again = makePrimaryButton("Calculate Again");
        again.setAlignmentX(CENTER_ALIGNMENT);
        again.addActionListener(e -> cardLayout.show(root, "MENU"));

        JButton restart = makeSecondaryButton("Start Over");
        restart.setAlignmentX(CENTER_ALIGNMENT);
        restart.addActionListener(e -> {
            nameField.setText("");
            userName = "";
            cardLayout.show(root, "WELCOME");
            nameField.requestFocusInWindow();
        });

        JButton exit = makeSecondaryButton("Exit");
        exit.setAlignmentX(CENTER_ALIGNMENT);
        exit.addActionListener(e -> System.exit(0));

        card.add(icon);
        card.add(vgap(14));
        card.add(thankNameLabel);
        card.add(vgap(10));
        card.add(msg);
        card.add(vgap(8));
        card.add(thankSumLabel);
        card.add(vgap(28));
        card.add(sep);
        card.add(vgap(22));
        card.add(bye);
        card.add(vgap(28));
        card.add(again);
        card.add(vgap(10));
        card.add(restart);
        card.add(vgap(10));
        card.add(exit);

        addCenteredCard(thankPanel, card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private void addCenteredCard(JPanel container, JPanel card) {
        container.add(card, centeredCardConstraints());
    }
    private GridBagConstraints centeredCardConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.insets = new Insets(40, 40, 40, 40);
        return gbc;
    }

    private JPanel makeCard() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                // inky comic-panel outline
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(2, 2, getWidth()-4, getHeight()-4, 24, 24));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JLabel makeLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font(COMIC_FONT, Font.BOLD, size));
        registerScaled(l, size, Font.PLAIN);
        return l;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField f = new JTextField(placeholder) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setOpaque(false);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(new Font(COMIC_FONT, Font.BOLD, 16));
        registerScaled(f, 16, Font.PLAIN);
        resetFieldBorder(f);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(10, ACCENT, 2),
                    new EmptyBorder(10, 14, 10, 14)));
            }
            @Override public void focusLost(FocusEvent e) { resetFieldBorder(f); }
        });
        return f;
    }

    private void resetFieldBorder(JTextField f) {
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, FIELD_BD, 2),
            new EmptyBorder(10, 14, 10, 14)));
    }
    private void errorFieldBorder(JTextField f) {
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, DANGER, 2),
            new EmptyBorder(10, 14, 10, 14)));
    }
    private void flashError(JTextField f) {
        errorFieldBorder(f);
        new Timer(900, e -> resetFieldBorder(f)) {{ setRepeats(false); }}.start();
    }

    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getBackground();
                if (getModel().isPressed())       g2.setColor(PRESS_DARK);
                else if (getModel().isRollover()) g2.setColor(HOVER_DARK);
                else                              g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth()-3f, getHeight()-3f, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font(COMIC_FONT, Font.BOLD, 14));
        registerScaled(btn, 14, Font.BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return btn;
    }

    private JButton makeSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())        g2.setColor(PRESS_LIGHT);
                else if (getModel().isRollover())  g2.setColor(HOVER_LIGHT);
                else                               g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth()-3f, getHeight()-3f, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(TEXT);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        registerScaled(btn, 14, Font.PLAIN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        return btn;
    }

    private Component vgap(int h) { return Box.createRigidArea(new Dimension(0, h)); }

    private void shake(JComponent comp) {
        int x = comp.getX();
        Timer t = new Timer(25, null);
        int[] offsets = {-8, 8, -6, 6, -4, 4, -2, 2, 0};
        final int[] idx = {0};
        t.addActionListener(e -> {
            if (idx[0] < offsets.length) {
                comp.setLocation(x + offsets[idx[0]++], comp.getY());
            } else {
                comp.setLocation(x, comp.getY());
                t.stop();
            }
        });
        t.start();
    }

    /** Track a component's font so Ctrl+/- can scale it. */
    private void registerScaled(JComponent c, int baseSize, int style) {
        ScaledFont sf = new ScaledFont(c, baseSize, style);
        scaledFonts.add(sf);
    }
    static class ScaledFont {
        final JComponent comp;
        final int baseSize, style;
        ScaledFont(JComponent c, int baseSize, int style) {
            this.comp = c; this.baseSize = baseSize; this.style = style;
        }
        void apply(float scale) {
            comp.setFont(new Font(COMIC_FONT, style,
                Math.max(8, Math.round(baseSize * scale))));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INNER CLASSES
    // ══════════════════════════════════════════════════════════════════════════

    /** Manga-style speech bubble: white fill, ink outline, little tail. */
    static class SpeechBubble extends JPanel {
        SpeechBubble(JComponent content) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(14, 22, 26, 22));  // extra bottom space for tail
            add(content, BorderLayout.CENTER);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int tailH = 12;
            int bodyH = h - tailH;
            int r = 18;

            // Bubble outline traced as a single closed path (so the stroke joins cleanly).
            Path2D path = new Path2D.Double();
            path.moveTo(r, 0);
            path.lineTo(w - r, 0);
            path.quadTo(w - 1, 0, w - 1, r);
            path.lineTo(w - 1, bodyH - r);
            path.quadTo(w - 1, bodyH, w - r, bodyH);
            // Walk along the bottom edge, dropping the little tail near the left third.
            int tailLeft  = w / 4;
            int tailRight = tailLeft + 22;
            int tailTipX  = tailLeft + 4;
            path.lineTo(tailRight, bodyH);
            path.lineTo(tailTipX,  h - 1);
            path.lineTo(tailLeft,  bodyH);
            path.lineTo(r, bodyH);
            path.quadTo(0, bodyH, 0, bodyH - r);
            path.lineTo(0, r);
            path.quadTo(0, 0, r, 0);
            path.closePath();

            g2.setColor(Color.WHITE);
            g2.fill(path);
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
            g2.dispose();
        }
    }

    /** Glass-pane overlay that pops a "BAM!" comic burst over a target component. */
    static class ComicBurst extends JComponent {
        private static final String[] WORDS = {
            "BAM!", "POW!", "WHAM!", "ZAP!", "BOOM!",
            "KAPOW!", "ZOWIE!", "WHACK!", "ZING!", "BOOYAH!"
        };
        private static final int DURATION_MS = 850;

        private final Random rand = new Random();
        private boolean active = false;
        private String word = "BAM!";
        private float scale = 0f, alpha = 1f;
        private double rotation = 0;
        private long startTime;
        private Timer timer;
        private Point center = new Point(0, 0);

        // ── Manga "panel zoom" transition state ────────────────────────────
        private boolean panelMode = false;
        private long panelStart;
        private Point panelFocus = new Point(0, 0);
        private float panelT = 0f;        // 0..1 progress
        private boolean panelMidFired = false;
        private Runnable panelMidCallback;
        private Timer panelTimer;

        ComicBurst() {
            setOpaque(false);
        }

        /** Click-through: never block input even while visible. */
        @Override public boolean contains(int x, int y) { return false; }

        /**
         * Manga "click into a panel" zoom: speed lines converge on `focus`,
         * `onMid` fires at the midpoint (use it to swap screens), then the
         * lines retract revealing the new panel.
         */
        void mangaPanelZoom(Point focus, Runnable onMid) {
            panelFocus = focus != null ? focus : new Point(getWidth()/2, getHeight()/2);
            panelStart = System.currentTimeMillis();
            panelT = 0f;
            panelMidFired = false;
            panelMidCallback = onMid;
            panelMode = true;
            setVisible(true);
            if (panelTimer != null) panelTimer.stop();
            panelTimer = new Timer(16, e -> tickPanel((Timer) e.getSource()));
            panelTimer.start();
            repaint();
        }

        private void tickPanel(Timer self) {
            long elapsed = System.currentTimeMillis() - panelStart;
            float t = elapsed / 420f;   // total transition duration
            panelT = Math.min(1f, t);
            if (!panelMidFired && panelT >= 0.5f) {
                panelMidFired = true;
                if (panelMidCallback != null) panelMidCallback.run();
            }
            if (panelT >= 1f) {
                panelMode = false;
                self.stop();
                if (!active) setVisible(false);
                repaint();
                return;
            }
            repaint();
        }

        /** Pop a random burst over `target` (in this glass pane's coords). */
        void popOver(JComponent target) { popOverWith(null, target); }

        /** Pop centered on the glass pane with a fixed word. */
        void popHere(String fixedWord) { popOverWith(fixedWord, null); }

        void popOverWith(String fixedWord, JComponent target) {
            if (target != null && target.isShowing() && this.isShowing()) {
                Point p = SwingUtilities.convertPoint(target,
                    target.getWidth() / 2, target.getHeight() / 2, this);
                center = p;
            } else {
                center = new Point(getWidth() / 2, getHeight() / 2);
            }
            word = (fixedWord != null) ? fixedWord : WORDS[rand.nextInt(WORDS.length)];
            rotation = (rand.nextDouble() - 0.5) * 0.45;  // ±13°
            startTime = System.currentTimeMillis();
            scale = 0f;
            alpha = 1f;
            active = true;
            setVisible(true);
            if (timer != null) timer.stop();
            timer = new Timer(16, e -> tick(((Timer) e.getSource())));
            timer.start();
            repaint();
        }

        private void tick(Timer self) {
            long elapsed = System.currentTimeMillis() - startTime;
            float t = elapsed / (float) DURATION_MS;
            if (t >= 1f) {
                active = false;
                self.stop();
                setVisible(false);
                repaint();
                return;
            }
            // Phase 1 (0–0.25): scale 0 → 1.25 (overshoot)
            // Phase 2 (0.25–0.40): scale 1.25 → 1.0 (settle)
            // Phase 3 (0.40–1.00): hold then fade alpha 1 → 0
            if (t < 0.25f) {
                scale = (t / 0.25f) * 1.25f;
                alpha = 1f;
            } else if (t < 0.40f) {
                scale = 1.25f - ((t - 0.25f) / 0.15f) * 0.25f;
                alpha = 1f;
            } else {
                scale = 1.0f;
                alpha = 1f - ((t - 0.40f) / 0.60f);
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            if (!active && !panelMode) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // ─ Manga panel-zoom: focus lines converge then retract ─────────
            if (panelMode) {
                paintPanelZoom((Graphics2D) g2.create());
            }

            if (!active) { g2.dispose(); return; }

            float a = Math.max(0f, Math.min(1f, alpha));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

            g2.translate(center.x, center.y);

            // Action lines radiating outward (drawn before scale/rotate so they always feel big)
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 12; i++) {
                double ang = i * (Math.PI / 6) + rotation;
                int r1 = (int) (160 * scale);
                int r2 = (int) (210 * scale);
                int x1 = (int) (Math.cos(ang) * r1);
                int y1 = (int) (Math.sin(ang) * r1);
                int x2 = (int) (Math.cos(ang) * r2);
                int y2 = (int) (Math.sin(ang) * r2);
                g2.drawLine(x1, y1, x2, y2);
            }

            g2.rotate(rotation);
            g2.scale(scale, scale);

            // Starburst polygon — alternating long/short points
            int spikes = 14;
            double rOuter = 150, rInner = 95;
            Path2D star = new Path2D.Double();
            for (int i = 0; i < spikes * 2; i++) {
                double ang = i * Math.PI / spikes - Math.PI / 2;
                double r = (i % 2 == 0) ? rOuter : rInner;
                double x = Math.cos(ang) * r;
                double y = Math.sin(ang) * r;
                if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
            }
            star.closePath();

            // Fill yellow, ink outline
            g2.setColor(BURST_FILL);
            g2.fill(star);
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(star);

            // Inner highlight ring
            g2.setColor(new Color(255, 255, 255, 90));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new Ellipse2D.Double(-70, -60, 50, 28));

            // Big chunky text — ink outline + red fill
            Font f = new Font(COMIC_FONT, Font.BOLD, 60);
            FontRenderContext frc = g2.getFontRenderContext();
            TextLayout tl = new TextLayout(word, f, frc);
            Rectangle2D b = tl.getBounds();
            AffineTransform at = AffineTransform.getTranslateInstance(
                -b.getWidth() / 2 - b.getX(),
                b.getHeight() / 2 - b.getY() - b.getHeight() / 2);
            Shape outline = tl.getOutline(at);
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(outline);
            g2.setColor(BURST_TEXT);
            g2.fill(outline);

            g2.dispose();
        }

        /** Manga focus-line transition. closeT goes 0→1→0 over the animation. */
        private void paintPanelZoom(Graphics2D g2) {
            int w = getWidth(), h = getHeight();
            int maxR = (int) Math.hypot(w, h);
            float closeT = panelT < 0.5f ? (panelT * 2f) : (1f - (panelT - 0.5f) * 2f);

            // soft paper-white wash that grows then fades — sells the "zoom in" feel
            g2.setColor(new Color(245, 241, 232, Math.round(closeT * 0.30f * 255)));
            g2.fillRect(0, 0, w, h);

            int innerR = (int) (maxR * (1f - closeT) * 0.55f) + 40;
            int outerR = maxR;
            int lineCount = 42;
            float thickness = 2.5f + closeT * 1.5f;
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND,
                                         BasicStroke.JOIN_ROUND));
            for (int i = 0; i < lineCount; i++) {
                double ang = i * 2 * Math.PI / lineCount;
                int x1 = panelFocus.x + (int) (Math.cos(ang) * innerR);
                int y1 = panelFocus.y + (int) (Math.sin(ang) * innerR);
                int x2 = panelFocus.x + (int) (Math.cos(ang) * outerR);
                int y2 = panelFocus.y + (int) (Math.sin(ang) * outerR);
                g2.drawLine(x1, y1, x2, y2);
            }
            g2.dispose();
        }
    }

    /** Solid paper background overlaid with a halftone dot pattern (manga style). */
    static class GradientPanel extends JPanel {
        private final Color paper;
        private final TexturePaint halftone;
        GradientPanel(Color paper, Color dotColor) {
            this.paper = paper;
            this.halftone = makeHalftone(dotColor);
            setOpaque(true);
        }
        private static TexturePaint makeHalftone(Color dot) {
            int size = 12;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dot);
            g2.fillOval(size / 2 - 2, size / 2 - 2, 3, 3);
            g2.dispose();
            return new TexturePaint(img, new Rectangle(0, 0, size, size));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(paper);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setPaint(halftone);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        private final int thickness;
        RoundedBorder(int radius, Color color, int thickness) {
            this.radius = radius; this.color = color; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h - 2, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        UIManager.put("Panel.background",            BG);
        UIManager.put("TextField.background",        FIELD_BG);
        UIManager.put("TextField.foreground",        TEXT);
        UIManager.put("TextField.caretForeground",   ACCENT);
        UIManager.put("TextField.selectionColor",    ACCENT);
        UIManager.put("TextField.selectedTextColor", BG);
        UIManager.put("OptionPane.background",       CARD);
        UIManager.put("Button.background",           ACCENT);
        UIManager.put("Button.foreground",           BG);

        SwingUtilities.invokeLater(SumCalculator::new);
    }
}
