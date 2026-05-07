import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
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

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final Color BG       = new Color(13,  17,  30);
    private static final Color BG2      = new Color(18,  24,  45);
    private static final Color CARD     = new Color(24,  30,  50);
    private static final Color ACCENT   = new Color(99, 179, 237);
    private static final Color ACCENT2  = new Color(129, 140, 248);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color MUTED    = new Color(140, 152, 178);
    private static final Color SUCCESS  = new Color(52,  211, 153);
    private static final Color DANGER   = new Color(239, 68,  68);
    private static final Color FIELD_BG = new Color(30,  38,  60);
    private static final Color FIELD_BD = new Color(51,  65, 100);

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
                if (getModel().isPressed())       base = FIELD_BD;
                else if (getModel().isRollover()) base = FIELD_BG.brighter();
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(getModel().isRollover() ? ACCENT : FIELD_BD);
                g2.setStroke(new BasicStroke(1.5f));
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
        b.addActionListener(e -> openCalc(op));
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
            "<html><body style='text-align:center; width:380px'>"
                + op.description + "</body></html>",
            13, TEXT);
        desc.setAlignmentX(CENTER_ALIGNMENT);
        desc.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel hello = makeLabel("Hi " + userName + " — fill in the values:", 12, MUTED);
        hello.setAlignmentX(CENTER_ALIGNMENT);

        card.add(icon);
        card.add(vgap(4));
        card.add(title);
        card.add(vgap(6));
        card.add(formula);
        card.add(vgap(8));
        card.add(desc);
        card.add(vgap(6));
        card.add(hello);
        card.add(vgap(22));

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

        JLabel result = makeLabel("Result will appear here", 14, MUTED);
        result.setAlignmentX(CENTER_ALIGNMENT);
        result.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(result);
        card.add(vgap(20));

        JButton calcBtn  = makePrimaryButton("Calculate");
        JButton clearBtn = makeSecondaryButton("Clear");
        calcBtn.addActionListener(e -> doCompute(op, fields, result));
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
            e -> doCompute(op, fields, result));

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

    private void doCompute(Operation op, JTextField[] fields, JLabel result) {
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
    }

    private void goToThankWithoutResult() {
        thankNameLabel.setText("Goodbye" + (userName.isEmpty() ? "!" : ", " + userName + "!"));
        thankSumLabel.setText(lastOpName.isEmpty()
                ? "Come back anytime."
                : "Last result — " + lastOpName + " = " + format(lastResult));
        cardLayout.show(root, "THANK");
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

        JLabel icon = makeLabel("🎉", 60, SUCCESS);
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
                g2.setColor(FIELD_BD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 24, 24));
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
                if (getModel().isPressed())       g2.setColor(base.darker());
                else if (getModel().isRollover()) g2.setColor(base.brighter());
                else                              g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBackground(ACCENT);
        btn.setForeground(BG);
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
                if (getModel().isPressed())        g2.setColor(FIELD_BD.darker());
                else if (getModel().isRollover())  g2.setColor(FIELD_BD.brighter());
                else                               g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(FIELD_BD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 12, 12));
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
    static class GradientPanel extends JPanel {
        private final Color top, bottom;
        GradientPanel(Color top, Color bottom) {
            this.top = top; this.bottom = bottom;
            setOpaque(true);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
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
