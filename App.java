import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.ArrayList;
import java.util.List;

public class App {
    // Team Colors
    private static final Color RAVENS_PURPLE = new Color(36, 23, 115);
    private static final Color RAVENS_GOLD = new Color(158, 124, 12);
    private static final Color RAVENS_BLACK = new Color(0, 0, 0);
    private static final Color BG_COLOR = new Color(250, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color DETAIL_BG = new Color(248, 248, 250);
    private static final Color HOVER_COLOR = new Color(245, 243, 255);
    
    private static List<ExpandableCard> allCards = new ArrayList<>();
    private static JTextField searchField;
    private static JPanel contentPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::createUI);
    }

    private static void createUI() {
        JFrame frame = new JFrame("Baltimore Ravens – Roster & Staff");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 900);

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BG_COLOR);

        // Header
        JPanel header = createHeader(frame);
        mainContainer.add(header, BorderLayout.NORTH);

        // Content panel with cards
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        populateContent(contentPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        frame.add(mainContainer);
        frame.setVisible(true);
        
        // Show welcome dialog AFTER frame is visible
        SwingUtilities.invokeLater(() -> showWelcomeDialog(frame));
    }

    private static JPanel createHeader(JFrame parentFrame) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(RAVENS_PURPLE);
        header.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // Title
        JLabel title = new JLabel("BALTIMORE RAVENS");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(RAVENS_GOLD);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("2024-25 Roster & Coaching Staff");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitle.setForeground(Color.WHITE);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));

        // Search and controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setBackground(RAVENS_PURPLE);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = new JTextField(25);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RAVENS_GOLD, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterContent(searchField.getText());
            }
        });

        JButton expandAll = createControlButton("Expand All");
        expandAll.addActionListener(e -> toggleAll(true));

        JButton collapseAll = createControlButton("Collapse All");
        collapseAll.addActionListener(e -> toggleAll(false));

        JButton aboutBtn = createControlButton("About");
        aboutBtn.addActionListener(e -> showAboutDialog(parentFrame));

        JButton helpBtn = createControlButton("Help");
        helpBtn.addActionListener(e -> showHelpDialog(parentFrame));

        controls.add(new JLabel("Search: ") {{
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.PLAIN, 14));
        }});
        controls.add(searchField);
        controls.add(expandAll);
        controls.add(collapseAll);
        controls.add(aboutBtn);
        controls.add(helpBtn);

        header.add(title);
        header.add(subtitle);
        header.add(controls);

        return header;
    }

    private static JButton createControlButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.PLAIN, 13));
        btn.setBackground(RAVENS_GOLD);
        btn.setForeground(RAVENS_BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(RAVENS_GOLD.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(RAVENS_GOLD);
            }
        });
        
        return btn;
    }

    private static void populateContent(JPanel panel) {
        addSection(panel, "PLAYER ROSTER", RAVENS_GOLD);

        // Quarterbacks
        addSubsection(panel, "Quarterbacks");
        addPlayer(panel, "#8 Lamar Jackson", "QB", 
            "• NFL MVP: 2019, 2023\n• Career Passing Yards: 17,000+\n• Career Rushing Yards: 5,000+\n\nElite dual-threat quarterback and centerpiece of the Ravens offense.");
        addPlayer(panel, "#10 Cooper Rush", "QB",
            "• NFL Starts: 10+\n\nVeteran backup quarterback providing experience and stability.");

        // Running Backs
        addSubsection(panel, "Running Backs");
        addPlayer(panel, "#22 Derrick Henry", "RB",
            "• Career Rushing Yards: 9,000+\n• Career Rushing TDs: 90+\n\nPower back known for size, speed, and late-game dominance.");
        addPlayer(panel, "#43 Justice Hill", "RB",
            "• Versatile runner and receiver\n\nChange-of-pace back with strong special teams value.");
        addPlayer(panel, "#23 Keaton Mitchell", "RB",
            "• Yards per carry: 8.0+ (rookie season)\n\nExplosive speed back capable of breaking big plays.");
        addPlayer(panel, "#31 Rasheen Ali", "RB",
            "• College standout at Marshall\n\nYoung developmental back with quickness and vision.");
        addPlayer(panel, "#42 Patrick Ricard", "FB",
            "• Pro Bowls: Multiple\n\nVersatile fullback and blocking specialist, key to run-heavy sets.");

        // Tight Ends
        addSubsection(panel, "Tight Ends");
        addPlayer(panel, "#89 Mark Andrews", "TE",
            "• Pro Bowls: Multiple\n• Career Receptions: 400+\n\nPrimary receiving tight end and red-zone threat.");
        addPlayer(panel, "#80 Isaiah Likely", "TE",
            "• Breakout games when starting\n\nAthletic tight end with strong receiving upside.");
        addPlayer(panel, "#85 Charlie Kolar", "TE",
            "• Strong blocking metrics\n\nReliable tight end contributing in both run and pass schemes.");

        // Offensive Line
        addSubsection(panel, "Offensive Line");
        addPlayer(panel, "#79 Ronnie Stanley", "OT",
            "• Pro Bowl LT\n\nVeteran left tackle protecting the blind side.");
        addPlayer(panel, "#64 Tyler Linderbaum", "C",
            "• Pro Bowl Center\n\nElite technician anchoring the offensive line.");
        addPlayer(panel, "#72 Andrew Voorhees", "G",
            "• Rookie season contributor\n\nPhysical interior lineman with strong run-blocking ability.");
        addPlayer(panel, "#66 Ben Cleveland", "G",
            "• Known for size and power\n\nDepth guard providing physicality in the trenches.");
        addPlayer(panel, "#78 Corey Bullock", "OT",
            "• Developmental tackle\n\nYoung offensive lineman building versatility.");
        addPlayer(panel, "#70 Joseph Noteboom", "OT",
            "• NFL starter experience\n\nVeteran swing tackle offering depth and flexibility.");

        // Wide Receivers
        addSubsection(panel, "Wide Receivers");
        addPlayer(panel, "#4 Zay Flowers", "WR",
            "• 1,000+ receiving yards season\n\nExplosive playmaker and primary receiving threat.");
        addPlayer(panel, "#7 Rashod Bateman", "WR",
            "• Former 1st-round pick\n\nPolished route runner with strong hands.");
        addPlayer(panel, "#10 DeAndre Hopkins", "WR",
            "• All-Pro selections\n• Career receiving yards: 12,000+\n\nElite veteran receiver known for contested catches.");
        addPlayer(panel, "#12 Tylan Wallace", "WR",
            "• Special teams contributor\n\nDepth receiver with value in coverage units.");
        addPlayer(panel, "#86 Devontez Walker", "WR",
            "• College deep-threat specialist\n\nSpeed receiver capable of stretching defenses.");
        addPlayer(panel, "#16 LaJohntay Wester", "WR",
            "• Slot production at college level\n\nAgile receiver with return and slot potential.");

        // Defensive Front
        addSubsection(panel, "Defensive Line");
        addPlayer(panel, "#92 Nnamdi Madubuike", "DT",
            "• Double-digit sack season\n\nInterior disruptor and cornerstone of the defensive line.");
        addPlayer(panel, "#95 Travis Jones", "DT",
            "• Strong run-stopping metrics\n\nPower defensive tackle anchoring the interior.");
        addPlayer(panel, "#96 Broderick Washington Jr.", "DT",
            "• Reliable rotational lineman\n\nProvides depth and consistency up front.");
        addPlayer(panel, "#99 John Jenkins", "DT",
            "• Veteran NFL experience\n\nRun-stopping specialist with size and strength.");

        // Linebackers
        addSubsection(panel, "Linebackers");
        addPlayer(panel, "#18 Roquan Smith", "LB",
            "• All-Pro selections\n\nDefensive leader and tackling machine.");
        addPlayer(panel, "#49 Trenton Simpson", "LB",
            "• Speed and range metrics\n\nAthletic linebacker with sideline-to-sideline ability.");
        addPlayer(panel, "#53 Kyle Van Noy", "OLB",
            "• Veteran sacks and pressures\n\nExperienced edge defender and situational pass rusher.");

        // Secondary
        addSubsection(panel, "Secondary");
        addPlayer(panel, "#44 Marlon Humphrey", "CB",
            "• Pro Bowl CB\n\nPhysical cornerback and leader in the secondary.");
        addPlayer(panel, "#14 Kyle Hamilton", "SAF",
            "• All-Pro selection\n\nVersatile safety capable of playing all over the field.");

        // Specialists
        addSubsection(panel, "Specialists");
        addPlayer(panel, "#11 Jordan Stout", "P",
            "• Strong net punting average\n\nPrimary punter handling field-position duties.");
        addPlayer(panel, "#19 Tyler Loop", "K",
            "• College accuracy leader\n\nKicker responsible for field goals and kickoffs.");

        // COACHING STAFF
        addSection(panel, "COACHING STAFF", RAVENS_GOLD);

        addStaff(panel, "John Harbaugh", "Head Coach",
            "• Overall team leadership and vision\n• Game management and decision-making\n• Culture, discipline, and accountability\n\nSuper Bowl-winning head coach known for adaptability and long-term organizational stability.");
        addStaff(panel, "Todd Monken", "Offensive Coordinator",
            "• Offensive scheme design\n• Play-calling and game planning\n• Quarterback development\n\nArchitect of a modern, aggressive offense tailored to maximize player strengths.");
        addStaff(panel, "Zachary Orr", "Defensive Coordinator",
            "• Defensive play-calling\n• Defensive strategy and adjustments\n\nFormer Ravens linebacker bringing an attacking, player-first defensive philosophy.");
        addStaff(panel, "Chris Horton", "Special Teams Coordinator",
            "• Special teams schemes\n• Game-day execution\n\nOversees all kicking, coverage, and return units.");
        addStaff(panel, "Willie Taggart", "Assistant Head Coach / RB Coach",
            "• Assists head coach with leadership duties\n• Running back development\n\nProvides veteran leadership and develops the team's running back group.");
        addStaff(panel, "Tee Martin", "Quarterbacks Coach",
            "• Quarterback mechanics\n• Film study and decision-making\n\nWorks closely with quarterbacks to improve accuracy and command of the offense.");
        addStaff(panel, "Greg Lewis", "Wide Receivers Coach",
            "• Wide receiver technique\n• Route running and blocking\n\nDevelops wide receivers within the Ravens passing offense.");
        addStaff(panel, "George Godsey", "Tight Ends Coach",
            "• Tight end development\n• Blocking and route responsibilities\n\nWorks with tight ends on versatility in both the run and pass game.");
        addStaff(panel, "George Warhop", "Offensive Line Coach",
            "• Offensive line technique\n• Pass protection and run blocking\n\nDevelops offensive linemen and ensures cohesive line play.");
        addStaff(panel, "Travis Switzer", "Run Game Coordinator",
            "• Run blocking schemes\n• Coordination between OL, RBs, and TEs\n\nAligns run game concepts across offensive position groups.");
        addStaff(panel, "Dennis Johnson", "Defensive Line Coach",
            "• Defensive line technique\n• Run defense and pass rush\n\nCoaches interior and edge linemen to control the line of scrimmage.");
        addStaff(panel, "Tyler Santucci", "Inside Linebackers Coach",
            "• Inside linebacker fundamentals\n• Run fits and coverage assignments\n\nDevelops inside linebackers within the defensive scheme.");
        addStaff(panel, "Matt Robinson", "Outside Linebackers Coach",
            "• Edge rusher technique\n• Pass rush coordination\n\nFocuses on developing outside linebackers as pass rush threats.");
        addStaff(panel, "Chuck Smith", "Pass Rush Coach",
            "• Pass rush technique\n• Individual skill development\n\nSpecialist coach focusing on edge and interior pass rushing.");
        addStaff(panel, "Donald D'Alesio", "Defensive Backs Coach",
            "• Defensive back technique\n• Coverage scheme implementation\n\nDevelops cornerbacks and safeties within the defensive system.");
        addStaff(panel, "Chuck Pagano", "Senior Defensive Assistant",
            "• Defensive strategy advising\n• Secondary coaching support\n\nVeteran coach providing experience and insight to the defensive staff.");

        // FRONT OFFICE
        addSection(panel, "FRONT OFFICE", RAVENS_GOLD);

        addStaff(panel, "Eric DeCosta", "Executive VP & General Manager",
            "• Oversees all football operations\n• Final authority on roster construction\n• Contract negotiations and salary cap strategy\n\nLeads player personnel decisions and long-term team building strategy.");
        addStaff(panel, "Ozzie Newsome", "Executive Vice President",
            "• Senior football advisor\n• Organizational leadership\n\nHall of Fame executive providing guidance on football operations and culture.");
        addStaff(panel, "George Kokinis", "VP, Player Personnel",
            "• Manages pro and college scouting departments\n• Player acquisition strategy\n\nOversees evaluation of current players, free agents, and draft prospects.");
        addStaff(panel, "Nick Matteo", "VP, Football Administration",
            "• Contract administration\n• Salary cap compliance\n\nHandles financial and contractual logistics of football operations.");
        addStaff(panel, "David McDonald", "VP, Research & Development",
            "• Football analytics and data science\n• Decision support systems\n\nLeads analytics initiatives supporting scouting, strategy, and performance.");

        // MEDICAL & TRAINING
        addSection(panel, "MEDICAL & PERFORMANCE", RAVENS_GOLD);

        addStaff(panel, "Dr. Andrew Tucker", "Chief Medical Officer",
            "• Oversees all medical operations\n• Final authority on player health decisions\n\nLeads the Ravens' medical program and coordinates care across all specialties.");
        addStaff(panel, "Adrian Dixon", "Head Athletic Trainer",
            "• Manages athletic training staff\n• Injury prevention and evaluation\n• Game-day medical coordination\n\nPrimary liaison between coaching staff, players, and medical personnel.");
        addStaff(panel, "Scott Elliott", "Strength & Conditioning Coordinator",
            "• Oversees all strength and conditioning programs\n• Coordinates performance development across positions\n\nLeads the Ravens' physical development philosophy, focusing on strength, speed, and durability.");
    }

    private static void addSection(JPanel panel, String title, Color color) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        label.setForeground(color);
        label.setBorder(BorderFactory.createEmptyBorder(30, 0, 15, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private static void addSubsection(JPanel panel, String title) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(RAVENS_PURPLE);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private static void addPlayer(JPanel panel, String name, String position, String details) {
        ExpandableCard card = new ExpandableCard(name, position, details, true);
        allCards.add(card);
        panel.add(card);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private static void addStaff(JPanel panel, String name, String role, String details) {
        ExpandableCard card = new ExpandableCard(name, role, details, false);
        allCards.add(card);
        panel.add(card);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private static void filterContent(String query) {
        String lowerQuery = query.toLowerCase().trim();
        for (ExpandableCard card : allCards) {
            boolean matches = lowerQuery.isEmpty() || 
                card.searchText.toLowerCase().contains(lowerQuery);
            card.setVisible(matches);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private static void toggleAll(boolean expand) {
        for (ExpandableCard card : allCards) {
            if (card.isVisible()) {
                card.setExpanded(expand);
            }
        }
    }

    // JOptionPane dialogs
    private static void showWelcomeDialog(JFrame parent) {
        JOptionPane.showMessageDialog(parent,
            "Welcome to the Baltimore Ravens Roster App!\n\n" +
            "Browse the complete 2024-25 roster including:\n" +
            "• Players by position\n" +
            "• Coaching staff\n" +
            "• Front office\n" +
            "• Medical & performance staff\n\n" +
            "Use the search feature to find specific people quickly!",
            "Welcome to Ravens Roster",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showAboutDialog(JFrame parent) {
        String message = "Baltimore Ravens Roster Application\n" +
                        "Version 2.0\n\n" +
                        "Complete roster and staff directory for the 2024-25 season.\n\n" +
                        "Features:\n" +
                        "• Searchable player and staff database\n" +
                        "• Expandable detail cards\n" +
                        "• Position-based organization\n" +
                        "• Team colors and branding\n\n" +
                        "Go Ravens!";
        
        JOptionPane.showMessageDialog(parent, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showHelpDialog(JFrame parent) {
        String[] options = {"Search Help", "Navigation Help", "Close"};
        int choice = JOptionPane.showOptionDialog(parent,
            "What do you need help with?",
            "Help Menu",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (choice == 0) {
            JOptionPane.showMessageDialog(parent,
                "SEARCH HELP\n\n" +
                "• Type in the search box to filter results\n" +
                "• Search by name, position, or role\n" +
                "• Examples: 'Lamar', 'QB', 'Coach', 'Medical'\n" +
                "• Clear the search box to show all entries",
                "Search Help",
                JOptionPane.INFORMATION_MESSAGE);
        } else if (choice == 1) {
            JOptionPane.showMessageDialog(parent,
                "NAVIGATION HELP\n\n" +
                "• Click any card to expand and view details\n" +
                "• Click again to collapse\n" +
                "• Use 'Expand All' to open all visible cards\n" +
                "• Use 'Collapse All' to close all cards\n" +
                "• Scroll to browse all sections",
                "Navigation Help",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Custom expandable card component
    static class ExpandableCard extends JPanel {
        private JPanel detailPanel;
        private JLabel arrowLabel;
        private boolean expanded = false;
        String searchText;

        public ExpandableCard(String name, String subtitle, String details, boolean isPlayer) {
            this.searchText = name + " " + subtitle + " " + details;
            
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 235), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getMaximumSize().height));

            // Header (clickable)
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(CARD_BG);
            header.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
            header.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.setBackground(CARD_BG);

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 15));
            nameLabel.setForeground(RAVENS_BLACK);
            
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
            subtitleLabel.setForeground(new Color(100, 100, 120));
            subtitleLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

            leftPanel.add(nameLabel);
            leftPanel.add(subtitleLabel);

            arrowLabel = new JLabel("▼");
            arrowLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            arrowLabel.setForeground(RAVENS_GOLD);

            header.add(leftPanel, BorderLayout.CENTER);
            header.add(arrowLabel, BorderLayout.EAST);

            // Detail panel
            detailPanel = new JPanel(new BorderLayout());
            detailPanel.setBackground(DETAIL_BG);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(15, 18, 18, 18));
            detailPanel.setVisible(false);

            JTextArea detailText = new JTextArea(details);
            detailText.setEditable(false);
            detailText.setLineWrap(true);
            detailText.setWrapStyleWord(true);
            detailText.setBackground(DETAIL_BG);
            detailText.setFont(new Font("Arial", Font.PLAIN, 13));
            detailText.setForeground(new Color(60, 60, 70));

            detailPanel.add(detailText, BorderLayout.CENTER);

            // Hover effect
            header.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!expanded) header.setBackground(HOVER_COLOR);
                }
                public void mouseExited(MouseEvent e) {
                    if (!expanded) header.setBackground(CARD_BG);
                }
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Double-click shows quick info dialog
                        showQuickInfo(name, subtitle, details);
                    } else {
                        toggleExpand();
                    }
                }
            });

            add(header);
            add(detailPanel);
        }

        private void toggleExpand() {
            setExpanded(!expanded);
        }

        public void setExpanded(boolean expand) {
            this.expanded = expand;
            detailPanel.setVisible(expand);
            arrowLabel.setText(expand ? "▲" : "▼");
            revalidate();
        }

        private void showQuickInfo(String name, String subtitle, String details) {
            JOptionPane.showMessageDialog(null,
                name + " - " + subtitle + "\n\n" + details,
                "Quick Info",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}