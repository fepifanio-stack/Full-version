import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;


public class HotelManagementGame extends JFrame {

    

    static final Color BG_TOP   = new Color(14, 19, 34);
    static final Color BG_BOT   = new Color(32, 42, 74);
    static final Color PANEL    = new Color(26, 34, 56);
    static final Color PANEL2   = new Color(38, 49, 78);
    static final Color ACCENT   = new Color(70, 130, 225);
    static final Color GREEN    = new Color(55, 180, 120);
    static final Color GOLD     = new Color(240, 185, 60);
    static final Color RED      = new Color(220, 80, 80);
    static final Color WHITE    = new Color(240, 244, 250);
    static final Color GRAY     = new Color(165, 176, 195);

    static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    

    enum RoomType {
        SINGLE("Single", "\uD83D\uDECF", 1000),
        DOUBLE("Double", "\uD83D\uDECF\uD83D\uDECF", 1500),
        DELUXE("Deluxe", "\uD83D\uDC51", 2500);

        final String label;
        final String icon;
        final int price;

        RoomType(String label, String icon, int price) {
            this.label = label;
            this.icon = icon;
            this.price = price;
        }
    }

    enum State { AVAILABLE, OCCUPIED, DIRTY, LOCKED }

    static class Guest {
        String name;
        String face;
        RoomType wanted;
        int nights;
        int patience;
        int maxPatience;
        boolean vip;
    }

    static class Room {
        int number;
        RoomType type;
        State state;
        Guest guest;
        int daysLeft;
        int unlockCost;
    }

    static final String[] NAMES = {
        "Maria Santos", "Juan Dela Cruz", "Ana Reyes", "Miguel Garcia",
        "Sofia Lim", "Carlos Tan", "Bea Villanueva", "Rico Mendoza",
        "Lea Navarro", "Paolo Cruz", "Tina Ramos", "Jose Bautista",
        "Grace Aquino", "Dante Flores", "Mika Torres", "Noel Castillo"
    };

    static final String[] FACES = {
        "\uD83E\uDDD1", "\uD83D\uDC69", "\uD83D\uDC68", "\uD83E\uDDD3",
        "\uD83D\uDC75", "\uD83D\uDC74", "\uD83E\uDDD4", "\uD83D\uDC71"
    };

    static final String[] LEVEL_NAMES = {
        "Tiny Inn", "Cozy Inn", "Nice Hotel", "Luxury Resort", "Grand Palace"
    };
    static final int[] LEVEL_REQ = {0, 6000, 16000, 32000, 60000};

    // Upgrades: 0 = cleaner, 1 = marketing, 2 = decor, 3 = concierge
    static final String[] UPG_NAMES = {
        "\uD83E\uDDF9 Hire Cleaner", "\uD83D\uDCE3 Marketing Campaign",
        "\uD83C\uDFA8 Luxury Decor", "\uD83D\uDECE Concierge Desk"
    };
    static final String[] UPG_DESC = {
        "Cleans 2 dirty rooms per day (PHP 150/day)",
        "More guests arrive every day",
        "+10% room prices and +5 reputation",
        "Guests wait longer in the queue"
    };
    static final int[] UPG_BASE = {1500, 2500, 3000, 2000};
    static final int[] UPG_MAX  = {3, 3, 3, 2};

    static final int MAX_QUEUE = 6;

    int money = 10000;
    int reputation = 50;
    int level = 1;
    int day = 1;
    int totalGuests = 0;
    int totalEarned = 0;
    int[] upg = new int[4];

    final Random rnd = new Random();
    final ArrayList<Guest> queue = new ArrayList<>();
    final ArrayList<Room> rooms = new ArrayList<>();
    final ArrayList<RoomTile> tiles = new ArrayList<>();
    Guest selected = null;

    // ============================
    // UI REFERENCES
    // ============================

    JLabel moneyLbl, dayLbl, levelLbl, guestsLbl, statusLbl, queueTitle;
    JProgressBar repBar;
    JPanel queuePanel;
    JTextArea logArea;
    FancyButton[] shopBtns = new FancyButton[4];

    
    // CUSTOM UI COMPONENTS
    

    static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

   
    static class GradientPanel extends JPanel {
        GradientPanel(LayoutManager lm) {
            super(lm);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOT));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

   
    static class Card extends JPanel {
        final Color fill;
        final Color outline;

        Card(Color fill, Color outline) {
            this.fill = fill;
            this.outline = outline;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            if (outline != null) {
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    
    static class FancyButton extends JButton {
        final Color base;

        FancyButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(WHITE);
            setFont(font(Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            Color c = base;
            if (!isEnabled()) {
                c = new Color(62, 68, 86);
            } else if (getModel().isPressed()) {
                c = base.darker();
            } else if (getModel().isRollover()) {
                c = base.brighter();
            }
            g2.setPaint(new GradientPaint(0, 0, c, 0, getHeight(), c.darker()));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    
    // CONSTRUCTOR
   

    public HotelManagementGame() {
        setTitle("Sunrise Grand Hotel - Management Game");
        setSize(1340, 760);
        setMinimumSize(new Dimension(1200, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildRooms();
        buildGUI();

        for (int i = 0; i < 3; i++) {
            queue.add(newGuest());
        }

        log("Welcome to Sunrise Grand Hotel! Serve guests and grow your empire.");
        flash("Click a guest on the left, then click a vacant room.", GOLD);
        refreshAll();
    }

    void buildRooms() {
        RoomType[] rowTypes = {RoomType.SINGLE, RoomType.DOUBLE, RoomType.DELUXE};
        int[] unlockCosts = {1500, 2500, 4000};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                Room r = new Room();
                r.number = 101 + row * 4 + col;
                r.type = rowTypes[row];
                r.unlockCost = unlockCosts[row];
                boolean open = (row == 0 && col < 2) || col == 0;
                r.state = open ? State.AVAILABLE : State.LOCKED;
                rooms.add(r);
            }
        }
    }

    
    // BUILD GUI
    
    void buildGUI() {
        GradientPanel root = new GradientPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(createTopBar(), BorderLayout.NORTH);
        root.add(createQueuePanel(), BorderLayout.WEST);
        root.add(createRoomsPanel(), BorderLayout.CENTER);
        root.add(createSidePanel(), BorderLayout.EAST);
        root.add(createBottomBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    JLabel statLabel() {
        JLabel l = new JLabel();
        l.setForeground(WHITE);
        l.setFont(font(Font.BOLD, 15));
        return l;
    }

    JPanel chip(JLabel label, Color border) {
        Card c = new Card(PANEL2, border);
        c.setLayout(new BorderLayout());
        c.setBorder(new EmptyBorder(8, 14, 8, 14));
        c.add(label, BorderLayout.CENTER);
        return c;
    }

    JPanel createTopBar() {
        Card top = new Card(PANEL, null);
        top.setLayout(new BorderLayout());
        top.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("\uD83C\uDFE8 Sunrise Grand Hotel");
        title.setForeground(WHITE);
        title.setFont(font(Font.BOLD, 24));
        top.add(title, BorderLayout.WEST);

        moneyLbl = statLabel();
        dayLbl = statLabel();
        levelLbl = statLabel();
        guestsLbl = statLabel();

        repBar = new JProgressBar(0, 100);
        repBar.setStringPainted(true);
        repBar.setForeground(GOLD);
        repBar.setBackground(new Color(20, 26, 44));
        repBar.setFont(font(Font.BOLD, 13));
        repBar.setPreferredSize(new Dimension(190, 34));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        stats.setOpaque(false);
        stats.add(chip(moneyLbl, GREEN));
        stats.add(chip(dayLbl, ACCENT));
        stats.add(chip(levelLbl, GOLD));
        stats.add(chip(guestsLbl, PANEL2));
        stats.add(repBar);

        top.add(stats, BorderLayout.EAST);
        return top;
    }

    JPanel createQueuePanel() {
        Card left = new Card(PANEL, null);
        left.setLayout(new BorderLayout(0, 10));
        left.setBorder(new EmptyBorder(16, 14, 14, 14));
        left.setPreferredSize(new Dimension(310, 100));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        queueTitle = new JLabel();
        queueTitle.setForeground(WHITE);
        queueTitle.setFont(font(Font.BOLD, 19));

        JLabel hint = new JLabel("Pick a guest, then click a vacant room");
        hint.setForeground(GRAY);
        hint.setFont(font(Font.PLAIN, 12));

        header.add(queueTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(hint);
        left.add(header, BorderLayout.NORTH);

        queuePanel = new JPanel();
        queuePanel.setOpaque(false);
        queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(queuePanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        left.add(scroll, BorderLayout.CENTER);

        return left;
    }

    JPanel createRoomsPanel() {
        Card center = new Card(PANEL, null);
        center.setLayout(new BorderLayout(0, 12));
        center.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("\uD83C\uDFE8 Your Rooms");
        title.setForeground(WHITE);
        title.setFont(font(Font.BOLD, 19));

        JLabel legend = new JLabel(
                "\uD83D\uDFE9 Vacant   \uD83D\uDFE6 Occupied   \uD83D\uDFE7 Needs cleaning   \u2B1B Locked (click to buy)");
        legend.setForeground(GRAY);
        legend.setFont(font(Font.PLAIN, 12));

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(legend);
        center.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 4, 12, 12));
        grid.setOpaque(false);
        for (Room r : rooms) {
            RoomTile t = new RoomTile(r);
            tiles.add(t);
            grid.add(t);
        }
        center.add(grid, BorderLayout.CENTER);

        return center;
    }

    JPanel createSidePanel() {
        Card right = new Card(PANEL, null);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(16, 14, 14, 14));
        right.setPreferredSize(new Dimension(290, 100));

        JLabel title = new JLabel("\uD83D\uDEE0 Upgrades");
        title.setForeground(WHITE);
        title.setFont(font(Font.BOLD, 19));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(title);
        right.add(Box.createVerticalStrut(10));

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            FancyButton b = new FancyButton("", ACCENT);
            b.setFont(font(Font.PLAIN, 12));
            b.setPreferredSize(new Dimension(260, 74));
            b.setMaximumSize(new Dimension(260, 74));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setToolTipText(UPG_DESC[i]);
            b.addActionListener(e -> buy(idx));
            shopBtns[i] = b;
            right.add(b);
            right.add(Box.createVerticalStrut(8));
        }

        right.add(Box.createVerticalStrut(6));
        JLabel logTitle = new JLabel("\uD83D\uDCDC Activity Log");
        logTitle.setForeground(WHITE);
        logTitle.setFont(font(Font.BOLD, 16));
        logTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(logTitle);
        right.add(Box.createVerticalStrut(6));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(17, 22, 38));
        logArea.setForeground(GRAY);
        logArea.setFont(font(Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(null);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(sp);

        return right;
    }

    JPanel createBottomBar() {
        Card bottom = new Card(PANEL, null);
        bottom.setLayout(new BorderLayout(20, 0));
        bottom.setBorder(new EmptyBorder(10, 20, 10, 12));

        statusLbl = new JLabel(" ");
        statusLbl.setFont(font(Font.BOLD, 15));
        statusLbl.setForeground(GOLD);
        bottom.add(statusLbl, BorderLayout.CENTER);

        FancyButton next = new FancyButton("\uD83C\uDF19  END DAY  \u25B6", GOLD);
        next.setForeground(new Color(35, 28, 8));
        next.setFont(font(Font.BOLD, 17));
        next.setPreferredSize(new Dimension(230, 52));
        next.addActionListener(e -> nextDay());
        bottom.add(next, BorderLayout.EAST);

        return bottom;
    }

    
    // ROOM TILE (custom painted)
   

    class RoomTile extends JPanel {
        final Room r;
        boolean hover = false;

        RoomTile(Room r) {
            this.r = r;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    onRoomClick(RoomTile.this.r);
                }
            });
        }

        private void centered(Graphics2D g2, String s, int y) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();
            int h = getHeight();

            Color top;
            Color bot;
            switch (r.state) {
                case AVAILABLE:
                    top = new Color(60, 172, 122);
                    bot = new Color(35, 120, 86);
                    break;
                case OCCUPIED:
                    top = new Color(78, 134, 228);
                    bot = new Color(45, 85, 170);
                    break;
                case DIRTY:
                    top = new Color(218, 142, 62);
                    bot = new Color(158, 94, 36);
                    break;
                default:
                    top = new Color(70, 78, 98);
                    bot = new Color(44, 51, 70);
                    break;
            }

            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 24, 24);

            boolean canPlace = selected != null && r.state == State.AVAILABLE;
            if (canPlace) {
                g2.setColor(GOLD);
                g2.setStroke(new BasicStroke(hover ? 4f : 2f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 22, 22);
            } else if (hover) {
                g2.setColor(new Color(255, 255, 255, 140));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 22, 22);
            }

            // header: room number + type icon
            g2.setColor(WHITE);
            g2.setFont(font(Font.BOLD, 15));
            g2.drawString("Room " + r.number, 12, 25);
            g2.setFont(font(Font.PLAIN, 16));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(r.type.icon, w - 12 - fm.stringWidth(r.type.icon), 26);

            int emojiY = (int) (h * 0.46);
            int line1Y = (int) (h * 0.60);
            int line2Y = (int) (h * 0.72);

            switch (r.state) {
                case AVAILABLE:
                    g2.setFont(font(Font.PLAIN, 32));
                    centered(g2, "\uD83D\uDD11", emojiY);
                    g2.setFont(font(Font.BOLD, 15));
                    centered(g2, "Vacant", line1Y);
                    break;
                case OCCUPIED:
                    g2.setFont(font(Font.PLAIN, 32));
                    centered(g2, r.guest.face, emojiY);
                    g2.setFont(font(Font.BOLD, 14));
                    centered(g2, r.guest.name.split(" ")[0] + (r.guest.vip ? " \u2B50" : ""), line1Y);
                    g2.setFont(font(Font.PLAIN, 12));
                    centered(g2, r.daysLeft + " night(s) left", line2Y);
                    double frac = r.daysLeft / (double) r.guest.nights;
                    int bx = 14;
                    int bw = w - 28;
                    int by = h - 30;
                    g2.setColor(new Color(255, 255, 255, 70));
                    g2.fillRoundRect(bx, by, bw, 8, 8, 8);
                    g2.setColor(WHITE);
                    g2.fillRoundRect(bx, by, (int) (bw * frac), 8, 8, 8);
                    break;
                case DIRTY:
                    g2.setFont(font(Font.PLAIN, 32));
                    centered(g2, "\uD83E\uDDF9", emojiY);
                    g2.setFont(font(Font.BOLD, 14));
                    centered(g2, "Needs cleaning", line1Y);
                    g2.setFont(font(Font.PLAIN, 12));
                    centered(g2, "Click: PHP 100", line2Y);
                    break;
                default:
                    g2.setFont(font(Font.PLAIN, 32));
                    centered(g2, "\uD83D\uDD12", emojiY);
                    g2.setFont(font(Font.BOLD, 14));
                    centered(g2, "Unlock", line1Y);
                    g2.setFont(font(Font.PLAIN, 12));
                    centered(g2, "PHP " + String.format("%,d", r.unlockCost), line2Y);
                    break;
            }

            // bottom label
            g2.setFont(font(Font.BOLD, 11));
            g2.setColor(new Color(255, 255, 255, 210));
            String bottomText = r.type.label;
            if (canPlace) {
                int v = compare(r, selected);
                bottomText = v == 0 ? "\u2714 Perfect match"
                        : v > 0 ? "\u2B06 Free upgrade!" : "\u2B07 Downgrade";
                g2.setColor(v < 0 ? new Color(255, 210, 210) : WHITE);
            }
            centered(g2, bottomText, h - 10);

            g2.dispose();
        }
    }

    
    // GUEST CARD (custom painted)
    

    class GuestCard extends JPanel {
        final Guest g;

        GuestCard(Guest g) {
            this.g = g;
            setOpaque(false);
            Dimension d = new Dimension(262, 104);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selected = (selected == GuestCard.this.g) ? null : GuestCard.this.g;
                    if (selected != null) {
                        flash("Now click a vacant room for " + selected.name + ".", GOLD);
                    }
                    rebuildQueue();
                    repaintTiles();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics gr) {
            Graphics2D g2 = (Graphics2D) gr.create();
            smooth(g2);
            boolean sel = (g == selected);

            g2.setColor(sel ? new Color(52, 68, 106) : PANEL2);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.setColor(sel ? GOLD : new Color(62, 76, 112));
            g2.setStroke(new BasicStroke(sel ? 3f : 1.5f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);

            g2.setFont(font(Font.PLAIN, 36));
            g2.setColor(WHITE);
            g2.drawString(g.face, 12, 56);

            g2.setFont(font(Font.BOLD, 15));
            g2.drawString(g.name, 62, 28);

            if (g.vip) {
                g2.setColor(GOLD);
                g2.setFont(font(Font.BOLD, 11));
                g2.drawString("\u2B50 VIP - pays 1.5x", 62, 44);
            }

            g2.setColor(GRAY);
            g2.setFont(font(Font.PLAIN, 12));
            g2.drawString("Wants: " + g.wanted.icon + " " + g.wanted.label
                    + " \u2022 " + g.nights + " night(s)", 62, 61);

            g2.setColor(GREEN);
            g2.setFont(font(Font.BOLD, 13));
            g2.drawString("Pays \u2248 PHP " + String.format("%,d", quote(g)), 62, 79);

            // patience hearts
            g2.setFont(font(Font.PLAIN, 14));
            int x = 14;
            for (int i = 0; i < g.maxPatience; i++) {
                g2.setColor(i < g.patience ? RED : new Color(75, 82, 102));
                g2.drawString("\u2764", x, 94);
                x += 18;
            }

            g2.dispose();
        }
    }

  
    // REFRESH HELPERS
    

    void refreshAll() {
        moneyLbl.setText("\uD83D\uDCB0 PHP " + String.format("%,d", money));
        dayLbl.setText("\uD83D\uDCC5 Day " + day);
        levelLbl.setText("\uD83C\uDFC6 " + LEVEL_NAMES[level - 1] + " (Lv " + level + ")");
        guestsLbl.setText("\uD83D\uDC65 " + totalGuests);
        moneyLbl.setForeground(money < 0 ? RED : WHITE);

        repBar.setValue(reputation);
        repBar.setString("\u2B50 Reputation " + reputation + "/100");

        if (level < LEVEL_REQ.length) {
            levelLbl.setToolTipText("Earn PHP "
                    + String.format("%,d", LEVEL_REQ[level] - totalEarned)
                    + " more to reach the next level");
        } else {
            levelLbl.setToolTipText("Maximum level reached!");
        }

        rebuildQueue();
        updateShop();
        repaintTiles();
    }

    void repaintTiles() {
        for (RoomTile t : tiles) {
            t.repaint();
        }
    }

    void rebuildQueue() {
        queuePanel.removeAll();
        for (Guest g : queue) {
            queuePanel.add(new GuestCard(g));
            queuePanel.add(Box.createVerticalStrut(10));
        }
        if (queue.isEmpty()) {
            JLabel empty = new JLabel(
                    "<html>\uD83D\uDE34 No guests waiting.<br>Press END DAY to attract more!</html>");
            empty.setForeground(GRAY);
            empty.setFont(font(Font.BOLD, 13));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            queuePanel.add(Box.createVerticalStrut(20));
            queuePanel.add(empty);
        }
        queueTitle.setText("\uD83D\uDECE Guest Queue (" + queue.size() + "/" + MAX_QUEUE + ")");
        queuePanel.revalidate();
        queuePanel.repaint();
    }

    void updateShop() {
        for (int i = 0; i < 4; i++) {
            boolean maxed = upg[i] >= UPG_MAX[i];
            String cost = maxed ? "MAXED OUT" : "PHP " + String.format("%,d", upgCost(i));
            shopBtns[i].setText("<html><center><b>" + UPG_NAMES[i] + "</b> ("
                    + upg[i] + "/" + UPG_MAX[i] + ")<br>"
                    + UPG_DESC[i] + "<br><b>" + cost + "</b></center></html>");
            shopBtns[i].setEnabled(!maxed && money >= upgCost(i));
        }
    }

    void log(String msg) {
        logArea.append("Day " + day + ": " + msg + "\n");
        if (logArea.getDocument().getLength() > 7000) {
            logArea.replaceRange("", 0, 2500);
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    void flash(String msg, Color c) {
        statusLbl.setText(msg);
        statusLbl.setForeground(c);
    }

    void changeRep(int delta) {
        reputation = Math.max(0, Math.min(100, reputation + delta));
    }

    
    // GAME LOGIC
   

    double mult(Guest g) {
        double m = 1.0 + upg[2] * 0.10;
        if (g.vip) {
            m *= 1.5;
        }
        return m;
    }

    int quote(Guest g) {
        return (int) Math.round(g.wanted.price * g.nights * mult(g));
    }

    /** 0 = exact match, >0 = upgrade for the guest, <0 = downgrade. */
    int compare(Room r, Guest g) {
        return r.type.ordinal() - g.wanted.ordinal();
    }

    int upgCost(int i) {
        return UPG_BASE[i] * (upg[i] + 1);
    }

    Guest newGuest() {
        Guest g = new Guest();
        g.name = NAMES[rnd.nextInt(NAMES.length)];
        g.face = FACES[rnd.nextInt(FACES.length)];

        int roll = rnd.nextInt(10) + (level - 1);
        g.wanted = roll < 5 ? RoomType.SINGLE : roll < 9 ? RoomType.DOUBLE : RoomType.DELUXE;

        g.nights = 1 + rnd.nextInt(4);
        g.maxPatience = 3 + upg[3];
        g.patience = g.maxPatience;
        g.vip = reputation >= 60 && rnd.nextInt(10) == 0;
        return g;
    }

    void onRoomClick(Room r) {
        switch (r.state) {
            case LOCKED:
                if (money >= r.unlockCost) {
                    int ok = JOptionPane.showConfirmDialog(this,
                            "Unlock Room " + r.number + " (" + r.type.label + ") for PHP "
                                    + String.format("%,d", r.unlockCost) + "?",
                            "Expand your hotel", JOptionPane.YES_NO_OPTION);
                    if (ok == JOptionPane.YES_OPTION) {
                        money -= r.unlockCost;
                        r.state = State.AVAILABLE;
                        log("\uD83D\uDD13 Unlocked Room " + r.number + ".");
                        flash("Room " + r.number + " is ready for guests!", GREEN);
                        refreshAll();
                    }
                } else {
                    flash("You need PHP " + String.format("%,d", r.unlockCost)
                            + " to unlock this room.", RED);
                }
                break;

            case DIRTY:
                if (money >= 100) {
                    money -= 100;
                    r.state = State.AVAILABLE;
                    flash("Room " + r.number + " cleaned (-PHP 100).", GREEN);
                    refreshAll();
                } else {
                    flash("Not enough money to clean the room!", RED);
                }
                break;

            case OCCUPIED:
                flash(r.guest.name + " is staying in Room " + r.number + " for "
                        + r.daysLeft + " more night(s).", GRAY);
                break;

            default:
                if (selected == null) {
                    flash("Pick a guest from the queue first!", GOLD);
                } else {
                    checkIn(r, selected);
                }
                break;
        }
    }

    void checkIn(Room r, Guest g) {
        int v = compare(r, g);
        int base;
        int repChange;
        String note;

        if (v == 0) {
            base = r.type.price;
            repChange = 3;
            note = "Perfect match!";
        } else if (v > 0) {
            base = g.wanted.price;
            repChange = 5;
            note = "Free upgrade - the guest is thrilled!";
        } else {
            base = (int) (r.type.price * 0.7);
            repChange = -3;
            note = "Guest wanted a better room and paid less.";
        }

        int pay = (int) Math.round(base * g.nights * mult(g));
        money += pay;
        totalEarned += pay;
        totalGuests++;
        changeRep(repChange);

        r.state = State.OCCUPIED;
        r.guest = g;
        r.daysLeft = g.nights;

        queue.remove(g);
        selected = null;

        log(g.name + " checked into Room " + r.number + " (+PHP " + String.format("%,d", pay) + ").");
        flash("\uD83D\uDCB0 +PHP " + String.format("%,d", pay) + "  " + note,
                v < 0 ? RED : GREEN);

        refreshAll();
        checkLevelUp();
    }

    void buy(int i) {
        if (upg[i] >= UPG_MAX[i]) {
            return;
        }
        int cost = upgCost(i);
        if (money < cost) {
            flash("Not enough money for that upgrade!", RED);
            return;
        }
        money -= cost;
        upg[i]++;
        if (i == 2) {
            changeRep(5);
        }
        log("Bought: " + UPG_NAMES[i] + " (level " + upg[i] + ").");
        flash(UPG_NAMES[i] + " upgraded!", GREEN);
        refreshAll();
    }

    void nextDay() {
        day++;

        // check-outs
        for (Room r : rooms) {
            if (r.state == State.OCCUPIED) {
                r.daysLeft--;
                if (r.daysLeft <= 0) {
                    int tip = rnd.nextInt(3) == 0 ? 0 : reputation * (1 + rnd.nextInt(4));
                    money += tip;
                    totalEarned += tip;
                    log(r.guest.name + " checked out of Room " + r.number
                            + (tip > 0 ? " and left a PHP " + tip + " tip!" : "."));
                    r.state = State.DIRTY;
                    r.guest = null;
                }
            }
        }

        // cleaners
        int capacity = upg[0] * 2;
        int cleaned = 0;
        for (Room r : rooms) {
            if (capacity > 0 && r.state == State.DIRTY) {
                r.state = State.AVAILABLE;
                capacity--;
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log("\uD83E\uDDF9 Cleaners prepared " + cleaned + " room(s).");
        }

        // daily expenses
        int open = 0;
        for (Room r : rooms) {
            if (r.state != State.LOCKED) {
                open++;
            }
        }
        int expenses = open * 40 + upg[0] * 150;
        money -= expenses;

        // impatient guests leave
        for (int i = queue.size() - 1; i >= 0; i--) {
            Guest g = queue.get(i);
            g.patience--;
            if (g.patience <= 0) {
                queue.remove(i);
                if (g == selected) {
                    selected = null;
                }
                changeRep(-4);
                log("\uD83D\uDE20 " + g.name + " got tired of waiting and left! (-4 reputation)");
            }
        }

        // new arrivals
        int arrivals = 1 + rnd.nextInt(2 + upg[1]) + (reputation >= 70 ? 1 : 0);
        arrivals = addGuests(arrivals);

        // random events
        int e = rnd.nextInt(100);
        if (e < 8) {
            addGuests(2);
            log("\uD83D\uDE8C A tour bus arrived with extra guests!");
        } else if (e < 15) {
            changeRep(4);
            log("\uD83D\uDCDD A guest posted a glowing review! (+4 reputation)");
        } else if (e < 21) {
            money -= 300;
            log("\uD83D\uDD27 A pipe burst and repairs cost PHP 300.");
        }

        flash("\u2600 Day " + day + " begins. Expenses paid: PHP "
                + String.format("%,d", expenses) + ".", GOLD);

        refreshAll();
        checkLevelUp();
        checkGameOver();
    }

    int addGuests(int count) {
        int added = 0;
        for (int i = 0; i < count; i++) {
            if (queue.size() < MAX_QUEUE) {
                queue.add(newGuest());
                added++;
            } else {
                changeRep(-1);
                log("The queue is full - a guest walked away. (-1 reputation)");
            }
        }
        return added;
    }

    void checkLevelUp() {
        int newLevel = 1;
        for (int i = 0; i < LEVEL_REQ.length; i++) {
            if (totalEarned >= LEVEL_REQ[i]) {
                newLevel = i + 1;
            }
        }
        if (newLevel > level) {
            level = newLevel;
            changeRep(5);
            log("\uD83C\uDFC6 Level up! Your hotel is now a " + LEVEL_NAMES[level - 1] + ".");
            refreshAll();
            String extra = level == LEVEL_NAMES.length
                    ? "\n\nYou reached the top - you are a hotel tycoon! Keep playing as long as you like."
                    : "\n\nHigher levels bring guests who want fancier rooms.";
            JOptionPane.showMessageDialog(this,
                    "\uD83C\uDF89 LEVEL UP!\n\nYour hotel is now a " + LEVEL_NAMES[level - 1] + "!" + extra,
                    "Hotel Level Up!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    void checkGameOver() {
        if (money >= -1000) {
            if (money < 0) {
                flash("\u26A0 You are in debt! Fill your rooms before it is too late.", RED);
            }
            return;
        }
        int choice = JOptionPane.showOptionDialog(this,
                "\uD83D\uDCB8 Your hotel went bankrupt on day " + day + "!\n\nGuests served: "
                        + totalGuests + "\nTotal earned: PHP " + String.format("%,d", totalEarned),
                "Game Over", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
                null, new String[]{"Play Again", "Quit"}, "Play Again");
        if (choice == 0) {
            dispose();
            SwingUtilities.invokeLater(() -> new HotelManagementGame().setVisible(true));
        } else {
            System.exit(0);
        }
    }

   
    // MAIN
   

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        SwingUtilities.invokeLater(() -> new HotelManagementGame().setVisible(true));
    }
}