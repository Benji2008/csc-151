// ============================================================
//  ConcreteEstimatorPro.java  –  Fayetteville NC Edition
//  JB Construction – Concrete Pad Estimator
//
//  LOCAL DATA (Fayetteville / Cumberland County, NC 2025):
//  • Concrete prices (HomeBlue 2025):
//      3,000 PSI  $122–$136/yd³
//      4,000 PSI  $130–$145/yd³
//      5,000 PSI  $139–$156/yd³
//      Delivery surcharge $120–$260/truck
//  • Labor (ZipRecruiter Fayetteville 2025):
//      Avg $19.24/hr | Range $15.67–$26.95/hr
//  • Local suppliers:
//      Quality Concrete Co.   (910) 483-7155
//      Concrete Service Co.   concreteservice.com   Est 1965
//      CCF Materials          ccfmaterials.com
//      S&W Ready Mix          (910) 496-3232  1309 S Reilly Rd
//  • Climate (WeatherSpark / BestPlaces):
//      Humid subtropical; avg annual 60°F; 219 sunny days/yr
//      Summers 89-91°F, humid; Winters 35-55°F, mild
//      ~45 in/yr rainfall, ~110 rain days/yr
// ============================================================

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.time.*;
import java.time.format.*;

public class ConcreteEstimatorPro extends JFrame {

    // ── Colours ───────────────────────────────────────────────
    private static final Color CLR_PRIMARY = new Color(0x1A3A5C);
    private static final Color CLR_ACCENT  = new Color(0xC8842A);
    private static final Color CLR_BG      = new Color(0xF2F4F5);
    private static final Color CLR_CARD    = Color.WHITE;
    private static final Color CLR_TEXT    = new Color(0x1C2833);
    private static final Color CLR_SUCCESS = new Color(0x1A6E3C);
    private static final Color CLR_WARN    = new Color(0xB7770D);
    private static final Color CLR_DANGER  = new Color(0xA93226);
    private static final Color CLR_MUTED   = new Color(0x717D7E);

    private static final Font FNT_TITLE  = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font FNT_HEADER = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FNT_BODY   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FNT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FNT_MONO   = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FNT_RESULT = new Font("Segoe UI", Font.BOLD,  17);

    private static final DecimalFormat DF  = new DecimalFormat("#,##0.00");
    private static final DecimalFormat DF1 = new DecimalFormat("#,##0.0");
    private static final DecimalFormat DF0 = new DecimalFormat("#,##0");

    private static final String FILE_PROJECTS = "projects.csv";
    private static final String FILE_LABOR    = "labor.csv";
    private static final String FILE_ESTIMATE = "estimate.txt";

    // ── Fayetteville NC concrete price table (2025) ──────────
    // { name, defaultThickness, 3kLo,3kHi, 4kLo,4kHi, 5kLo,5kHi }
    private static final Object[][] SLAB_DATA = {
        { "Residential Driveway",   "4",  122,136, 130,145, 139,156 },
        { "Warehouse Slab",         "6",  122,136, 130,145, 139,156 },
        { "Heavy Equipment Pad",    "8",  130,145, 139,156, 139,156 },
        { "Sidewalk / Walkway",     "4",  122,136, 122,136, 130,145 },
        { "Foundation Slab",        "6",  130,145, 139,156, 139,156 },
        { "Custom",                 "6",  122,136, 130,145, 139,156 },
    };

    // Local Fayetteville NC concrete suppliers
    private static final String[] SUPPLIERS = {
        "Quality Concrete Co., Inc. – (910) 483-7155",
        "Concrete Service Company – (Est. 1965)",
        "CCF Materials – ccfmaterials.com",
        "S&W Ready Mix – (910) 496-3232",
        "Other / Not Listed"
    };

    // Fayetteville monthly avg high temps °F (Jan–Dec)
    private static final int[] MONTHLY_HIGH = { 53,57,61,71,79,86,89,87,81,71,62,55 };
    // avg rain days per month
    private static final int[] RAIN_DAYS    = { 11,10,14,12,16,17,15,13,10, 8, 9,10 };

    // ── Input fields ──────────────────────────────────────────
    private JTextField txtProject, txtLocation, txtClientName, txtClientPhone;
    private JComboBox<String> cboSlabType, cboThickness, cboPSI, cboSupplier;
    private JTextField txtLength, txtWidth, txtWastePct;
    private JTextField txtWorkers, txtHours, txtDays, txtRate;
    private JTextField txtEquipCost, txtPermitCost, txtOtherCost;
    private JComboBox<String> cboDiscountType;
    private JTextField txtDiscountVal, txtTaxRate;
    private JComboBox<String> cboStartMonth;

    // ── Result labels ─────────────────────────────────────────
    private JLabel lblArea, lblVolume, lblConcretePrice;
    private JLabel lblMaterialCost, lblLaborCost, lblEquipCost;
    private JLabel lblOtherCost, lblSubtotal, lblDiscount, lblTax, lblTotalCost;
    private JLabel lblWeather, lblCuring;

    // ── History table ─────────────────────────────────────────
    private DefaultTableModel historyModel;
    private JTable historyTable;

    private double concretePricePerYd3 = 129.0;
    private float  uiScale             = 1.0f;

    // ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException |
               IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ignored) {}
        SwingUtilities.invokeLater(() -> new ConcreteEstimatorPro().setVisible(true));
    }

    public ConcreteEstimatorPro() {
        setTitle("Concrete Pad Estimator Pro  |  JB Construction – Fayetteville, NC");
        setSize(1020, 880);
        setMinimumSize(new Dimension(920, 760));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(CLR_BG);
        setLayout(new BorderLayout());
        addZoomSupport();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooterLabel(), BorderLayout.SOUTH);
        loadLastLabor();
        loadHistory();
        updatePriceFromPreset();
        updateClimateInfo();
    }

    // ═══════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(CLR_PRIMARY);
        hdr.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel logo  = new JLabel("JB CONSTRUCTION");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logo.setForeground(CLR_ACCENT);

        JLabel title = new JLabel("Concrete Pad Estimator Pro");
        title.setFont(FNT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Fayetteville  ·  Cumberland County, NC 28306");
        sub.setFont(FNT_SMALL);
        sub.setForeground(new Color(0xA9B7C6));

        JPanel left = new JPanel(new GridLayout(3, 1, 0, 2));
        left.setOpaque(false);
        left.add(logo); left.add(title); left.add(sub);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        JLabel dateLabel = new JLabel(dateStr + "  |  " + seasonBlurb(), SwingConstants.RIGHT);
        dateLabel.setFont(FNT_SMALL);
        dateLabel.setForeground(new Color(0xA9B7C6));

        JLabel phoneLabel = new JLabel("Quality Concrete Co. (910) 483-7155  |  Concrete Service Co. (1965)", SwingConstants.RIGHT);
        phoneLabel.setFont(FNT_SMALL);
        phoneLabel.setForeground(new Color(0xA9B7C6));

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 4));
        right.setOpaque(false);
        right.add(dateLabel); right.add(phoneLabel);

        hdr.add(left, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    private String seasonBlurb() {
        int m  = LocalDate.now().getMonthValue();
        int hi = MONTHLY_HIGH[m - 1];
        int rd = RAIN_DAYS[m - 1];
        String icon = (m>=6&&m<=8) ? "☀ Hot & humid" :
                      (m==12||m<=2)? "❄ Cool/mild"  :
                      (m>=3&&m<=5) ? "🌿 Mild spring": "🍂 Cool fall";
        return icon + " · avg high " + hi + "°F · ~" + rd + " rain days/mo";
    }

    // ═══════════════════════════════════════════════════════
    //  MAIN SPLIT
    // ═══════════════════════════════════════════════════════
    private JSplitPane buildCenter() {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapScroll(buildFormPanel()),
                buildRightPanel());
        sp.setDividerLocation(535);
        sp.setResizeWeight(0.55);
        sp.setBorder(null);
        return sp;
    }

    private JScrollPane wrapScroll(JPanel p) {
        JScrollPane sp = new JScrollPane(p,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ── LEFT form ─────────────────────────────────────────
    private JPanel buildFormPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 5));

        p.add(card("📋  Project Information",    buildProjectFields()));
        p.add(Box.createVerticalStrut(7));
        p.add(card("📐  Slab Dimensions",         buildDimensionFields()));
        p.add(Box.createVerticalStrut(7));
        p.add(card("🏗  Concrete Specification",  buildConcreteSpecFields()));
        p.add(Box.createVerticalStrut(7));
        p.add(card("👷  Labor  —  Fayetteville avg $19.24/hr (2025)", buildLaborFields()));
        p.add(Box.createVerticalStrut(7));
        p.add(card("💰  Additional Costs",        buildAdditionalCostFields()));
        p.add(Box.createVerticalStrut(7));
        p.add(card("🏷  Discount & Tax",          buildDiscountFields()));
        p.add(Box.createVerticalStrut(7));
        p.add(card("🌤  Project Timing  —  Fayetteville NC Climate", buildClimateFields()));
        p.add(Box.createVerticalStrut(10));
        p.add(buildButtonRow());
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private JPanel buildProjectFields() {
        JPanel g = grid(4, 2);
        txtProject     = field();
        txtLocation    = field("Fayetteville, NC 28306");
        txtClientName  = field();
        txtClientPhone = field();
        g.add(lbl("Project Name:"  )); g.add(txtProject);
        g.add(lbl("Job Location:"  )); g.add(txtLocation);
        g.add(lbl("Client Name:"   )); g.add(txtClientName);
        g.add(lbl("Client Phone:"  )); g.add(txtClientPhone);
        return g;
    }

    private JPanel buildDimensionFields() {
        JPanel g = grid(4, 2);
        cboSlabType = new JComboBox<>();
        for (Object[] row : SLAB_DATA) cboSlabType.addItem((String) row[0]);
        style(cboSlabType);
        cboSlabType.addActionListener(e -> updatePriceFromPreset());

        cboThickness = new JComboBox<>(new String[]{"3","4","5","6","7","8","10","12"});
        cboThickness.setSelectedItem("6");
        style(cboThickness);

        txtLength = field("100");
        txtWidth  = field("150");

        g.add(lbl("Slab Type:"));      g.add(cboSlabType);
        g.add(lbl("Length (ft):"));    g.add(txtLength);
        g.add(lbl("Width (ft):"));     g.add(txtWidth);
        g.add(lbl("Thickness (in):")); g.add(cboThickness);
        return g;
    }

    private JPanel buildConcreteSpecFields() {
        JPanel g = grid(4, 2);

        cboPSI = new JComboBox<>(new String[]{
            "3,000 PSI  –  Standard  ($122–$136/yd³)",
            "4,000 PSI  –  Commercial  ($130–$145/yd³)",
            "5,000 PSI  –  Heavy Duty  ($139–$156/yd³)"
        });
        style(cboPSI);
        cboPSI.addActionListener(e -> updatePriceFromPreset());

        cboSupplier = new JComboBox<>(SUPPLIERS);
        style(cboSupplier);

        txtWastePct = field("10");

        lblConcretePrice = new JLabel("–");
        lblConcretePrice.setFont(FNT_HEADER);
        lblConcretePrice.setForeground(CLR_SUCCESS);

        g.add(lbl("Concrete PSI:"));       g.add(cboPSI);
        g.add(lbl("Local Supplier:"));     g.add(cboSupplier);
        g.add(lbl("Waste Factor %:"));     g.add(txtWastePct);
        g.add(lbl("Est. Price / yd³:"));   g.add(lblConcretePrice);
        return g;
    }

    private JPanel buildLaborFields() {
        JPanel g = grid(5, 2);
        txtWorkers = field("4");
        txtHours   = field("8");
        txtDays    = field("5");
        txtRate    = field("19");

        JLabel note = new JLabel(
            "<html><i>Fayetteville 2025: avg $19.24 · range $15.67–$26.95/hr (ZipRecruiter)</i></html>");
        note.setFont(FNT_SMALL);
        note.setForeground(CLR_MUTED);

        g.add(lbl("# of Workers:"));    g.add(txtWorkers);
        g.add(lbl("Hours / Day:"));     g.add(txtHours);
        g.add(lbl("Working Days:"));    g.add(txtDays);
        g.add(lbl("Hourly Rate ($):")); g.add(txtRate);
        g.add(new JLabel(""));          g.add(note);
        return g;
    }

    private JPanel buildAdditionalCostFields() {
        JPanel g = grid(3, 2);
        txtEquipCost  = field("0");
        txtPermitCost = field("0");
        txtOtherCost  = field("0");
        g.add(lbl("Equipment / Rental ($):")); g.add(txtEquipCost);
        g.add(lbl("Permits / Fees ($):"));     g.add(txtPermitCost);
        g.add(lbl("Other Costs ($):"));        g.add(txtOtherCost);
        return g;
    }

    private JPanel buildDiscountFields() {
        JPanel g = grid(3, 2);
        cboDiscountType = new JComboBox<>(new String[]{"Percentage (%)","Fixed Amount ($)"});
        style(cboDiscountType);
        txtDiscountVal = field("0");
        txtTaxRate     = field("0");
        g.add(lbl("Discount Type:"));  g.add(cboDiscountType);
        g.add(lbl("Discount Value:")); g.add(txtDiscountVal);
        g.add(lbl("Tax Rate %:"));     g.add(txtTaxRate);
        return g;
    }

    private JPanel buildClimateFields() {
        JPanel g = grid(3, 2);
        String[] months = {"January","February","March","April","May","June",
                           "July","August","September","October","November","December"};
        cboStartMonth = new JComboBox<>(months);
        cboStartMonth.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        style(cboStartMonth);
        cboStartMonth.addActionListener(e -> updateClimateInfo());

        lblWeather = new JLabel("–"); lblWeather.setFont(FNT_SMALL);
        lblCuring  = new JLabel("–"); lblCuring.setFont(FNT_SMALL);
        lblCuring.setForeground(CLR_WARN);

        g.add(lbl("Pour Month:"));            g.add(cboStartMonth);
        g.add(lbl("Fayetteville Climate:"));  g.add(lblWeather);
        g.add(lbl("Curing Advisory:"));       g.add(lblCuring);
        return g;
    }

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.add(actionBtn("⚡  Calculate",   CLR_PRIMARY, e -> calculate()));
        row.add(actionBtn("🖨  Print/Save",  CLR_SUCCESS,  e -> printEstimate()));
        row.add(actionBtn("🗑  Clear Form",  CLR_DANGER,   e -> clearForm()));
        return row;
    }

    // ── RIGHT panel ────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 5, 10, 10));
        p.add(card("📊  Estimate Summary", buildSummaryGrid()));
        p.add(Box.createVerticalStrut(8));
        p.add(buildHistoryCard());
        p.add(Box.createVerticalStrut(8));
        p.add(buildSupplierCard());
        return p;
    }

    private JPanel buildSummaryGrid() {
        JPanel g = new JPanel(new GridLayout(10, 2, 6, 5));
        g.setOpaque(false);
        lblArea         = rLbl("–");
        lblVolume       = rLbl("–");
        lblMaterialCost = rLbl("–");
        lblLaborCost    = rLbl("–");
        lblEquipCost    = rLbl("–");
        lblOtherCost    = rLbl("–");
        lblSubtotal     = rLbl("–");
        lblDiscount     = rLbl("–");
        lblTax          = rLbl("–");
        lblTotalCost    = new JLabel("–");
        lblTotalCost.setFont(FNT_RESULT);
        lblTotalCost.setForeground(CLR_TEXT);

        g.add(hdr("Area (sq ft):"       )); g.add(lblArea);
        g.add(hdr("Concrete Volume:"    )); g.add(lblVolume);
        g.add(hdr("Material Cost:"      )); g.add(lblMaterialCost);
        g.add(hdr("Labor Cost:"         )); g.add(lblLaborCost);
        g.add(hdr("Equipment + Permits:")); g.add(lblEquipCost);
        g.add(hdr("Other Costs:"        )); g.add(lblOtherCost);
        g.add(hdr("Subtotal:"           )); g.add(lblSubtotal);
        g.add(hdr("Discount:"           )); g.add(lblDiscount);
        g.add(hdr("Tax:"                )); g.add(lblTax);
        g.add(hdr("TOTAL:"              )); g.add(lblTotalCost);
        return g;
    }

    private JPanel buildHistoryCard() {
        String[] cols = {"Date","Project","Client","Slab Type","Size (ft)","Thickness","PSI","Area","Volume","Material","Labor","Equip+Permits","Other","Subtotal","Discount","Tax","TOTAL"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setFont(FNT_MONO);
        historyTable.setRowHeight(26);
        historyTable.setBackground(CLR_CARD);
        historyTable.setGridColor(new Color(0xE8EAED));
        historyTable.setShowVerticalLines(true);
        historyTable.setIntercellSpacing(new Dimension(6, 0));
        historyTable.setSelectionBackground(new Color(0xD6EAF8));
        historyTable.setSelectionForeground(CLR_TEXT);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Alternating row colours + bold green TOTAL column
        historyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    setBackground(new Color(0xD6EAF8));
                    setForeground(CLR_TEXT);
                } else if (col == 16) {  // TOTAL column
                    setBackground(new Color(0xEAF9EA));
                    setForeground(CLR_SUCCESS);
                    setFont(FNT_HEADER);
                } else {
                    setBackground(row % 2 == 0 ? CLR_CARD : new Color(0xF4F6F7));
                    setForeground(CLR_TEXT);
                    setFont(FNT_MONO);
                }
                setBorder(new EmptyBorder(2, 6, 2, 6));
                return this;
            }
        });

        JTableHeader th = historyTable.getTableHeader();
        th.setFont(FNT_HEADER);
        th.setBackground(CLR_PRIMARY);
        th.setForeground(Color.WHITE);
        th.setPreferredSize(new Dimension(0, 28));
        ((DefaultTableCellRenderer) th.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        int[] widths = {90,140,110,130,80,80,80,75,75,100,100,110,80,100,90,80,110};
        for (int i = 0; i < widths.length; i++)
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(historyTable);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xD5D8DC)));
        sp.setPreferredSize(new Dimension(455, 200));
        sp.getHorizontalScrollBar().setUnitIncrement(20);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        // Tooltip on hover showing full row summary
        historyTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = historyTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < historyModel.getRowCount()) {
                    String tip = "<html><b>" + historyModel.getValueAt(row,1) + "</b>"
                        + " &nbsp;|&nbsp; " + historyModel.getValueAt(row,3)
                        + " &nbsp;|&nbsp; " + historyModel.getValueAt(row,4) + " ft"
                        + " &nbsp;|&nbsp; " + historyModel.getValueAt(row,5) + "\" thick"
                        + " &nbsp;|&nbsp; " + historyModel.getValueAt(row,6)
                        + "<br>Material: " + historyModel.getValueAt(row,9)
                        + " &nbsp; Labor: " + historyModel.getValueAt(row,10)
                        + " &nbsp; <b>TOTAL: " + historyModel.getValueAt(row,16) + "</b></html>";
                    historyTable.setToolTipText(tip);
                }
            }
        });

        JButton btnDel = smallBtn("🗑 Delete Row", CLR_DANGER, e -> deleteSelectedRow());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel t = new JLabel("📁  Project History  —  scroll right for full details");
        t.setFont(FNT_HEADER); t.setForeground(CLR_PRIMARY);
        t.setBorder(new EmptyBorder(0, 0, 6, 0));
        top.add(t, BorderLayout.WEST);
        top.add(btnDel, BorderLayout.EAST);

        JPanel card = cardBase();
        card.add(top, BorderLayout.NORTH);
        card.add(sp,  BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));
        return card;
    }

    private JPanel buildSupplierCard() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        Object[][] refs = {
            { "Quality Concrete Co., Inc.",  "(910) 483-7155",  "qualityconcretenc.net  |  DOT-certified" },
            { "Concrete Service Company",    "concreteservice.com", "Est. 1965  |  Central & SE NC"       },
            { "CCF Materials",               "ccfmaterials.com",    "Fayetteville & surrounding areas"    },
            { "S&W Ready Mix",               "(910) 496-3232",  "1309 S. Reilly Rd, Fayetteville 28314"  },
        };

        for (Object[] r : refs) {
            JLabel l = new JLabel("<html><b>" + r[0] + "</b>  <font color='gray'>"
                    + r[1] + " · " + r[2] + "</font></html>");
            l.setFont(FNT_SMALL);
            l.setBorder(new EmptyBorder(2, 0, 2, 0));
            inner.add(l);
        }

        JLabel priceNote = new JLabel(
            "<html><i>2025 rates:  3k PSI $122–$136  |  4k PSI $130–$145  |  5k PSI $139–$156 per yd³</i></html>");
        priceNote.setFont(FNT_SMALL);
        priceNote.setForeground(CLR_MUTED);
        priceNote.setBorder(new EmptyBorder(6, 0, 0, 0));
        inner.add(priceNote);

        JLabel climNote = new JLabel(
            "<html><i>Fayetteville: 219 sunny days/yr · 45 in/yr rain · Summers 89-91°F humid</i></html>");
        climNote.setFont(FNT_SMALL);
        climNote.setForeground(CLR_MUTED);
        climNote.setBorder(new EmptyBorder(3, 0, 0, 0));
        inner.add(climNote);

        return card("📞  Fayetteville NC Suppliers & Reference Data", inner);
    }

    private JLabel buildFooterLabel() {
        JLabel f = new JLabel(
            "  JB Construction · Fayetteville, NC 28306  |  " +
            "Labor avg $19.24/hr · Concrete $122–$156/yd³ (2025)  |  " +
            "Climate: humid subtropical – hot summers, mild winters – plan curing accordingly",
            SwingConstants.CENTER);
        f.setFont(FNT_SMALL); f.setForeground(Color.WHITE); f.setOpaque(true);
        f.setBackground(CLR_PRIMARY.darker());
        f.setBorder(new EmptyBorder(6, 0, 6, 0));
        return f;
    }

    // ═══════════════════════════════════════════════════════
    //  BUSINESS LOGIC
    // ═══════════════════════════════════════════════════════
    private void calculate() {
        try {
            double L  = parse(txtLength);
            double W  = parse(txtWidth);
            double T  = Double.parseDouble((String) cboThickness.getSelectedItem()) / 12.0;
            double wp = parse(txtWastePct) / 100.0;

            double areaSqFt  = L * W;
            double volYd3    = (areaSqFt * T / 27.0) * (1 + wp);
            double matCost   = volYd3 * concretePricePerYd3;

            double laborCost = parse(txtWorkers) * parse(txtHours) * parse(txtDays) * parse(txtRate);
            double equipCost = parse(txtEquipCost) + parse(txtPermitCost);
            double otherCost = parse(txtOtherCost);
            double subtotal  = matCost + laborCost + equipCost + otherCost;

            double discAmt;
            if (cboDiscountType.getSelectedIndex() == 0)
                discAmt = subtotal * (parse(txtDiscountVal) / 100.0);
            else
                discAmt = parse(txtDiscountVal);

            double afterDisc = subtotal - discAmt;
            double taxAmt    = afterDisc * (parse(txtTaxRate) / 100.0);
            double total     = afterDisc + taxAmt;

            lblArea.setText(DF0.format(areaSqFt) + " ft²");
            lblVolume.setText(DF1.format(volYd3) + " yd³");
            lblMaterialCost.setText("$" + DF.format(matCost));
            lblLaborCost.setText("$"    + DF.format(laborCost));
            lblEquipCost.setText("$"    + DF.format(equipCost));
            lblOtherCost.setText("$"    + DF.format(otherCost));
            lblSubtotal.setText("$"     + DF.format(subtotal));
            lblDiscount.setText("−$"    + DF.format(discAmt));
            lblTax.setText("$"          + DF.format(taxAmt));
            lblTotalCost.setText("$"    + DF.format(total));
            lblTotalCost.setForeground(CLR_SUCCESS);

            saveProjectCSV(matCost, laborCost, equipCost, otherCost, subtotal, discAmt, taxAmt, total);
            saveLastLabor();
            loadHistory();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please fill in all numeric fields correctly.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updatePriceFromPreset() {
        if (cboSlabType == null || cboPSI == null) return;
        int si = cboSlabType.getSelectedIndex();
        if (si < 0 || si >= SLAB_DATA.length) return;
        Object[] row = SLAB_DATA[si];
        cboThickness.setSelectedItem(row[1]);

        int pi = cboPSI.getSelectedIndex();
        int lo = (int) row[2 + pi * 2];
        int hi = (int) row[3 + pi * 2];
        concretePricePerYd3 = (lo + hi) / 2.0;
        if (lblConcretePrice != null)
            lblConcretePrice.setText("$" + DF.format(concretePricePerYd3)
                    + "/yd³   (range $" + lo + "–$" + hi + ")");
    }

    private void updateClimateInfo() {
        if (cboStartMonth == null || lblWeather == null) return;
        int idx = cboStartMonth.getSelectedIndex();
        int hi  = MONTHLY_HIGH[idx];
        int rd  = RAIN_DAYS[idx];

        String weather, curing;
        if (hi >= 86) {
            // Fayetteville summer: June–August 86–89°F, high humidity
            weather = "<html>🌡 Avg high <b>" + hi + "°F</b>  ·  ~" + rd
                    + " rain days  ·  <b>High humidity (Fayetteville summer)</b></html>";
            curing  = "<html><font color='#B7770D'>⚠ Hot-weather pour: schedule for early AM,"
                    + " use set retarder, mist cure every 2–3 hrs, shade if possible</font></html>";
        } else if (hi <= 57) {
            // Dec–Feb: Fayetteville mild winters, rarely freezing but possible
            weather = "<html>🌨 Avg high <b>" + hi + "°F</b>  ·  ~" + rd
                    + " rain days  ·  Mild winter (Fayetteville avg)</html>";
            curing  = "<html><font color='#2471A3'>❄ Cool-weather cure: insulate if below 40°F,"
                    + " allow 20–25% extra cure time; NC winters usually mild</font></html>";
        } else {
            // Spring/Fall ideal
            weather = "<html>✅ Avg high <b>" + hi + "°F</b>  ·  ~" + rd
                    + " rain days  ·  Ideal Fayetteville conditions</html>";
            curing  = "<html><font color='#1A6E3C'>✅ Best months to pour in Fayetteville."
                    + " Standard 7-day wet cure. Full strength ~28 days.</font></html>";
        }
        lblWeather.setText(weather);
        lblCuring.setText(curing);
    }

    // ═══════════════════════════════════════════════════════
    //  FILE I/O
    // ═══════════════════════════════════════════════════════
    private void saveProjectCSV(double mat, double labor, double equip, double other,
                                double subtotal, double disc, double tax, double total) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PROJECTS, true))) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            String size = txtLength.getText() + "x" + txtWidth.getText();
            String thick = (String) cboThickness.getSelectedItem();
            String psi = ((String) cboPSI.getSelectedItem()).split("–")[0].trim();
            double areaSqFt = Double.parseDouble(txtLength.getText().trim())
                            * Double.parseDouble(txtWidth.getText().trim());
            double T = Double.parseDouble(thick) / 12.0;
            double wp = Double.parseDouble(txtWastePct.getText().trim()) / 100.0;
            double vol = (areaSqFt * T / 27.0) * (1 + wp);
            pw.println(
                date + ","
                + txtProject.getText().replace(",", ";") + ","
                + txtClientName.getText().replace(",", ";") + ","
                + ((String) cboSlabType.getSelectedItem()).replace(",", ";") + ","
                + size + ","
                + thick + "in,"
                + psi + ","
                + DF0.format(areaSqFt) + " ft²,"
                + DF1.format(vol) + " yd³,"
                + "$" + DF.format(mat) + ","
                + "$" + DF.format(labor) + ","
                + "$" + DF.format(equip) + ","
                + "$" + DF.format(other) + ","
                + "$" + DF.format(subtotal) + ","
                + "-$" + DF.format(disc) + ","
                + "$" + DF.format(tax) + ","
                + "$" + DF.format(total)
            );
        } catch (Exception ignored) {}
    }

    private void loadHistory() {
        historyModel.setRowCount(0);
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PROJECTS))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 17) {
                    // New format — load all columns directly
                    historyModel.addRow(parts);
                } else if (parts.length >= 5) {
                    // Old 5-column format — pad missing columns with "–"
                    String[] row = new String[17];
                    row[0] = parts[0]; // date
                    row[1] = parts[1]; // project
                    row[2] = "–";      // client
                    row[3] = "–";      // slab type
                    row[4] = "–";      // size
                    row[5] = "–";      // thickness
                    row[6] = "–";      // psi
                    row[7] = "–";      // area
                    row[8] = "–";      // volume
                    row[9]  = parts[2]; // material
                    row[10] = parts[3]; // labor
                    row[11] = "–";
                    row[12] = "–";
                    row[13] = "–";
                    row[14] = "–";
                    row[15] = "–";
                    row[16] = parts[4]; // total
                    historyModel.addRow(row);
                }
            }
        } catch (Exception ignored) {}
    }

    private void deleteSelectedRow() {
        int row = historyTable.getSelectedRow();
        if (row < 0) return;
        if (JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        historyModel.removeRow(row);
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_PROJECTS, false))) {
            for (int i = 0; i < historyModel.getRowCount(); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < historyModel.getColumnCount(); j++) {
                    if (j > 0) sb.append(",");
                    Object val = historyModel.getValueAt(i, j);
                    sb.append(val == null ? "" : val.toString());
                }
                pw.println(sb);
            }
        } catch (Exception ignored) {}
    }

    private void saveLastLabor() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_LABOR))) {
            pw.println(txtWorkers.getText() + "," + txtHours.getText()
                     + "," + txtDays.getText() + "," + txtRate.getText());
        } catch (Exception ignored) {}
    }

    private void loadLastLabor() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_LABOR))) {
            String[] d = br.readLine().split(",");
            if (d.length >= 4) {
                txtWorkers.setText(d[0]); txtHours.setText(d[1]);
                txtDays.setText(d[2]);    txtRate.setText(d[3]);
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════
    //  PRINT / SAVE
    // ═══════════════════════════════════════════════════════
    private void printEstimate() {
        String date    = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String expDate = LocalDate.now().plusDays(30)
                                  .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String supplier = (String) cboSupplier.getSelectedItem();
        String psiTxt   = ((String) cboPSI.getSelectedItem()).split("–")[0].trim();
        int mo = cboStartMonth.getSelectedIndex();
        String climRaw = lblCuring.getText().replaceAll("<[^>]+>","").replaceAll("&nbsp;"," ").trim();

        String report = """
=================================================================
            JB CONSTRUCTION  \u2013  CONCRETE PAD ESTIMATE
             Fayetteville, NC  |  Cumberland County
=================================================================
 Date Issued  : %s
 Valid Until  : %s  (30-day estimate)
 Project      : %s
 Location     : %s
 Client       : %s  |  %s
-----------------------------------------------------------------
 SLAB DETAILS
   Type          : %s
   Dimensions    : %s ft  x  %s ft
   Thickness     : %s inches
   Spec          : %s
   Waste Factor  : %s%%
   Area          : %s
   Volume Needed : %s
   Est. Unit $   : %s
   Supplier Ref  : %s
-----------------------------------------------------------------
 FAYETTEVILLE NC CLIMATE  \u2013  %s
   Avg High      : %s\u00b0F
   Avg Rain Days : ~%s days this month
   Cure Advisory : %s
-----------------------------------------------------------------
 COST BREAKDOWN
   Material / Concrete     :  %s
   Labor                   :  %s
   Equipment & Permits     :  %s
   Other Costs             :  %s
   Subtotal                :  %s
   Discount                :  %s
   Tax                     :  %s
-----------------------------------------------------------------
   TOTAL PROJECT COST      :  %s
=================================================================
 NOTES:
   * Prices based on Fayetteville NC 2025 market rates
   * Labor avg $19.24/hr (ZipRecruiter Fayetteville 2025)
   * Concrete $122\u2013$156/yd\u00b3 depending on PSI (HomeBlue 2025)
   * Concrete reaches ~70%% strength at 7 days, full at 28 days
   * This estimate is valid 30 days from date of issue
=================================================================
 Generated by Concrete Pad Estimator Pro  \u2013  JB Construction
=================================================================
""".formatted(
                date, expDate,
                txtProject.getText(), txtLocation.getText(),
                txtClientName.getText(), txtClientPhone.getText(),
                cboSlabType.getSelectedItem(),
                txtLength.getText(), txtWidth.getText(),
                cboThickness.getSelectedItem(), psiTxt,
                txtWastePct.getText(),
                lblArea.getText(), lblVolume.getText(), lblConcretePrice.getText(),
                supplier,
                cboStartMonth.getSelectedItem(),
                MONTHLY_HIGH[mo], RAIN_DAYS[mo], climRaw,
                lblMaterialCost.getText(), lblLaborCost.getText(),
                lblEquipCost.getText(), lblOtherCost.getText(),
                lblSubtotal.getText(), lblDiscount.getText(), lblTax.getText(),
                lblTotalCost.getText()
        );

        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_ESTIMATE))) {
            out.print(report);
        } catch (Exception ignored) {}

        JTextArea ta = new JTextArea(report);
        ta.setFont(FNT_MONO); ta.setEditable(false); ta.setBackground(CLR_BG);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(600, 530));

        int choice = JOptionPane.showOptionDialog(this, sp, "Estimate Preview – JB Construction",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{"✅ Save & Close", "❌ Cancel"}, "✅ Save & Close");

        if (choice == 0)
            JOptionPane.showMessageDialog(this,
                "Estimate saved to " + FILE_ESTIMATE, "Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm() {
        txtProject.setText(""); txtClientName.setText(""); txtClientPhone.setText("");
        txtLength.setText("100"); txtWidth.setText("150"); txtWastePct.setText("10");
        txtEquipCost.setText("0"); txtPermitCost.setText("0"); txtOtherCost.setText("0");
        txtDiscountVal.setText("0"); txtTaxRate.setText("0");
        lblArea.setText("–"); lblVolume.setText("–"); lblMaterialCost.setText("–");
        lblLaborCost.setText("–"); lblEquipCost.setText("–"); lblOtherCost.setText("–");
        lblSubtotal.setText("–"); lblDiscount.setText("–"); lblTax.setText("–");
        lblTotalCost.setText("–"); lblTotalCost.setForeground(CLR_TEXT);
    }

    // ═══════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════
    private JPanel card(String title, JPanel content) {
        JPanel p = cardBase();
        JLabel hdrLabel = new JLabel(title);
        hdrLabel.setFont(FNT_HEADER); hdrLabel.setForeground(CLR_PRIMARY);
        hdrLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,2,0, CLR_ACCENT),
                new EmptyBorder(0,0,6,0)));
        p.add(hdrLabel, BorderLayout.NORTH);
        p.add(content,  BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 55));
        return p;
    }

    private JPanel cardBase() {
        JPanel p = new JPanel(new BorderLayout(0,8));
        p.setBackground(CLR_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD5D8DC)),
                new EmptyBorder(10,12,12,12)));
        return p;
    }

    private JPanel grid(int rows, int cols) {
        JPanel p = new JPanel(new GridLayout(rows, cols, 8, 6));
        p.setOpaque(false); return p;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t); l.setFont(FNT_BODY); l.setForeground(CLR_TEXT); return l;
    }
    private JLabel hdr(String t) {
        JLabel l = new JLabel(t); l.setFont(FNT_HEADER); l.setForeground(CLR_TEXT); return l;
    }
    private JLabel rLbl(String t) {
        JLabel l = new JLabel(t); l.setFont(FNT_BODY); l.setForeground(CLR_TEXT); return l;
    }

    private JTextField field() { return field(""); }
    private JTextField field(String d) {
        JTextField tf = new JTextField(d);
        tf.setFont(FNT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xABB2B9)),
                new EmptyBorder(4,6,4,6)));
        return tf;
    }

    private void style(JComboBox<?> cb) { cb.setFont(FNT_BODY); cb.setBackground(CLR_CARD); }

    private JButton actionBtn(String label, Color bg, ActionListener al) {
        JButton b = new JButton(label);
        b.setFont(FNT_HEADER); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(155, 38));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.brighter()); }
            @Override
            public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private JButton smallBtn(String label, Color bg, ActionListener al) {
        JButton b = new JButton(label);
        b.setFont(FNT_SMALL); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    // ═══════════════════════════════════════════════════════
    //  CTRL + SCROLL ZOOM
    // ═══════════════════════════════════════════════════════
    private void addZoomSupport() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseWheelEvent mwe) {
                if ((mwe.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    float delta = mwe.getWheelRotation() < 0 ? 0.1f : -0.1f;
                    uiScale = Math.max(0.6f, Math.min(2.5f, uiScale + delta));
                    applyZoom(getContentPane(), uiScale);
                    revalidate();
                    repaint();
                }
            }
        }, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    private void applyZoom(Component c, float scale) {
        Font base = c.getFont();
        if (base != null) {
            // Derive a scaled version of whatever font the component currently has,
            // but anchor to the original point size stored in the font name mapping
            float newSize = Math.round(getBaseSize(base) * scale);
            c.setFont(base.deriveFont(Math.max(8f, newSize)));
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents())
                applyZoom(child, scale);
        }
    }

    /** Returns the "design" size for a font by looking at its style/size bucket. */
    private float getBaseSize(Font f) {
        // Map by style+name to original sizes used at design time
        String n = f.getName();
        boolean bold = f.isBold();
        if (n.contains("Consolas"))    return 12f;
        if (bold && f.getSize() >= 16) return 20f; // FNT_TITLE / FNT_RESULT
        if (bold)                      return 13f; // FNT_HEADER
        if (f.getSize() <= 11)         return 11f; // FNT_SMALL
        return 12f;                                // FNT_BODY
    }

    private double parse(JTextField tf) { return Double.parseDouble(tf.getText().trim()); }
}
