package net.runelite.client.plugins.loottracker;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootTrackerPanel extends PluginPanel {

    private static final int SLOT_SIZE = 40;
    private static final Color BG_DARK    = ColorScheme.DARK_GRAY_COLOR;
    private static final Color BG_HEADER  = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color BG_HOVER   = ColorScheme.DARKER_GRAY_HOVER_COLOR;
    private static final Color BG_SLOT        = new Color(30, 30, 30);
    private static final Color BG_SLOT_BORDER = new Color(60, 60, 60);
    private static final Color COL_ORANGE = new Color(0xFF981F);
    private static final Color COL_QTY    = new Color(0xFFFF00);

    private final LootTrackerConfig config;
    private final LootTrackerItemIconCache iconCache;
    private final Map<String, Boolean> collapsedState = new HashMap<>();

    private enum SortOrder { RECENT, VALUE, KILLS, RESET }
    private SortOrder sortOrder = SortOrder.RECENT;
    private List<LootTrackerRecord> lastRecords = new ArrayList<>();

    private final JLabel totalValueLabel = new JLabel("Total value: 0 gp");
    private final JLabel totalCountLabel = new JLabel("Total count: 0");
    private final JButton clearButton = new JButton("Clear");
    private Runnable clearRunnable;
    private final JPanel contentPanel;

    // Timer to refresh icons as they load
    private final Timer iconRefreshTimer;

    LootTrackerPanel(LootTrackerConfig config, LootTrackerItemIconCache iconCache) {
        super(false);
        this.config = config;
        this.iconCache = iconCache;

        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        // Top bar: icon + stats + clear button all in one clean row
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_DARK);
        topBar.setBorder(new EmptyBorder(8, 10, 8, 10));

        totalCountLabel.setForeground(Color.WHITE);
        totalCountLabel.setFont(FontManager.getRunescapeSmallFont());
        totalValueLabel.setForeground(Color.WHITE);
        totalValueLabel.setFont(FontManager.getRunescapeSmallFont());

        // Icon
        JLabel iconLabel = new JLabel();
        try {
            java.awt.image.BufferedImage icon = net.runelite.client.util.ImageUtil.loadImageResource(getClass(), "/loot_tracker_icon.png");
            if (icon != null) {
                java.awt.Image scaled = icon.getScaledInstance(28, 28, java.awt.Image.SCALE_SMOOTH);
                iconLabel.setIcon(new javax.swing.ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 8));

        // Stats
        JPanel textInfo = new JPanel();
        textInfo.setLayout(new BoxLayout(textInfo, BoxLayout.Y_AXIS));
        textInfo.setBackground(BG_DARK);
        textInfo.add(totalCountLabel);
        textInfo.add(totalValueLabel);

        JPanel leftSide = new JPanel(new BorderLayout());
        leftSide.setBackground(BG_DARK);
        leftSide.add(iconLabel, BorderLayout.WEST);
        leftSide.add(textInfo, BorderLayout.CENTER);

        // Clear button — subtle, top-right
        clearButton.setBackground(new Color(55, 45, 35));
        clearButton.setForeground(new Color(180, 180, 180));
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.setFont(FontManager.getRunescapeSmallFont());
        clearButton.setPreferredSize(new Dimension(45, 18));

        topBar.add(leftSide, BorderLayout.WEST);

        // Sort bar
        JPanel sortBar = new JPanel(new GridLayout(1, 4, 2, 0));
        sortBar.setBackground(BG_DARK);
        sortBar.setBorder(new EmptyBorder(0, 10, 8, 10));

        for (SortOrder order : SortOrder.values()) {
            String label = order == SortOrder.RESET ? "Reset" : order.name().charAt(0) + order.name().substring(1).toLowerCase();
            JButton btn = new JButton(label);
            btn.setBackground(order == sortOrder ? new Color(70, 60, 45) : ColorScheme.DARKER_GRAY_COLOR);
            btn.setForeground(order == sortOrder ? COL_ORANGE : Color.LIGHT_GRAY);
            btn.setFocusPainted(false);
            btn.setBorder(new EmptyBorder(3, 0, 3, 0));
            btn.setFont(FontManager.getRunescapeSmallFont());
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                if (order == SortOrder.RESET) {
                    lastRecords = new ArrayList<>();
                    if (clearRunnable != null) clearRunnable.run();
                    return;
                }
                sortOrder = order;
                // Reset button styles
                for (Component c : sortBar.getComponents()) {
                    if (c instanceof JButton) {
                        c.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                        ((JButton) c).setForeground(Color.LIGHT_GRAY);
                    }
                }
                btn.setBackground(new Color(70, 60, 45));
                btn.setForeground(COL_ORANGE);
                updateRecords(lastRecords);
            });
            sortBar.add(btn);
        }

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_DARK);

        JScrollPane scroll = new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(BG_DARK);
        northPanel.add(topBar, BorderLayout.NORTH);
        northPanel.add(sortBar, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Refresh panel every 200ms to show newly loaded icons
        iconRefreshTimer = new Timer(200, e -> contentPanel.repaint());
        iconRefreshTimer.start();
    }

    void addClearListener(Runnable listener) {
        this.clearRunnable = listener;
        clearButton.addActionListener(e -> listener.run());
    }

    void updateRecords(List<LootTrackerRecord> records) {
        this.lastRecords = records;
        SwingUtilities.invokeLater(() -> {
            int total = records.stream().mapToInt(LootTrackerRecord::getTotalValue).sum();
            int count = records.stream().mapToInt(LootTrackerRecord::getKills).sum();
            totalValueLabel.setText("Total value: " + QuantityFormatter.quantityToStackSize(total) + " gp");
            totalCountLabel.setText("Total count: " + QuantityFormatter.quantityToStackSize(count));

            // Sort
            List<LootTrackerRecord> sorted = new ArrayList<>(records);
            switch (sortOrder) {
                case VALUE:  sorted.sort((a, b) -> Integer.compare(b.getTotalValue(), a.getTotalValue())); break;
                case KILLS:  sorted.sort((a, b) -> Integer.compare(b.getKills(), a.getKills())); break;
                case RESET:  break; // handled separately
                case RECENT: // already in recent order
                default: break;
            }

            contentPanel.removeAll();
            for (LootTrackerRecord r : sorted) {
                addSectionToPanel(r);
            }
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    private void addSectionToPanel(LootTrackerRecord record) {
        boolean collapsed = collapsedState.getOrDefault(record.getName(), false);

        // Header
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(BG_HEADER);
        header.setBorder(new EmptyBorder(8, 10, 8, 10));
        header.setCursor(new Cursor(Cursor.HAND_CURSOR));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String killText = config.showKillCount() && record.getKills() > 0
                ? " x " + record.getKills() : "";

        int totalVal = record.getTotalValue();
        String valueText = totalVal > 0 ? " (" + QuantityFormatter.quantityToStackSize(totalVal) + " gp)" : "";

        JLabel nameLabel = new JLabel(record.getName() + killText + valueText);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel arrow = new JLabel(collapsed ? "▶" : "▼");
        arrow.setForeground(Color.GRAY);
        arrow.setFont(new Font("SansSerif", Font.PLAIN, 10));
        arrow.setPreferredSize(new Dimension(16, 14));
        arrow.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.weightx = 1.0;
        header.add(nameLabel, c1);
        header.add(arrow, new GridBagConstraints());

        // Items grid panel
        // Sort items by total value descending
        List<LootTrackerItem> sortedItems = new ArrayList<>(record.getItems());
        sortedItems.sort((a, b) -> Integer.compare(b.getTotalValue(), a.getTotalValue()));
        JPanel itemsGrid = buildItemsGrid(sortedItems);
        itemsGrid.setVisible(!collapsed);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(55, 55, 55));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean now = !collapsedState.getOrDefault(record.getName(), false);
                collapsedState.put(record.getName(), now);
                itemsGrid.setVisible(!now);
                arrow.setText(now ? "▶" : "▼");
                contentPanel.revalidate();
                contentPanel.repaint();
            }
            @Override public void mouseEntered(MouseEvent e) { header.setBackground(BG_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { header.setBackground(BG_HEADER); }
        });

        contentPanel.add(header);
        contentPanel.add(itemsGrid);
        contentPanel.add(sep);
    }

    private JPanel buildItemsGrid(List<LootTrackerItem> items) {
        // Use GridLayout with fixed columns for predictable sizing
        int cols = 5;
        int rows = (int) Math.ceil(items.size() / (double) cols);
        int cellSize = SLOT_SIZE + 4;

        JPanel grid = new JPanel(new GridLayout(rows, cols, 4, 4));
        grid.setBackground(BG_DARK);
        grid.setBorder(new EmptyBorder(4, 6, 4, 6));
        grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * cellSize + 8));

        for (LootTrackerItem item : items) {
            grid.add(buildItemSlot(item));
        }
        // Fill empty slots to complete the grid
        int remainder = (rows * cols) - items.size();
        for (int i = 0; i < remainder; i++) {
            JPanel empty = new JPanel();
            empty.setBackground(BG_DARK);
            empty.setPreferredSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
            grid.add(empty);
        }
        return grid;
    }

    private JPanel buildItemSlot(LootTrackerItem item) {
        JPanel slot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Slot background
                g.setColor(BG_SLOT);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(BG_SLOT_BORDER);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                // Item icon
                BufferedImage icon = iconCache.get(item.getId());
                if (icon != null && icon != LootTrackerItemIconCache.LOADING) {
                    int x = (SLOT_SIZE - icon.getWidth()) / 2;
                    int y = (SLOT_SIZE - icon.getHeight()) / 2;
                    g.drawImage(icon, x, y, null);
                }

                // Quantity top-left in yellow
                if (item.getQuantity() > 1) {
                    String qty = QuantityFormatter.quantityToStackSize(item.getQuantity());
                    g.setFont(FontManager.getRunescapeSmallFont());
                    // Shadow
                    g.setColor(Color.BLACK);
                    g.drawString(qty, 3, 12);
                    g.setColor(COL_QTY);
                    g.drawString(qty, 2, 11);
                }
            }
        };

        slot.setPreferredSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
        String tooltip = item.getName();
        if (item.getQuantity() > 1) tooltip += " ×" + QuantityFormatter.quantityToStackSize(item.getQuantity());
        if (item.getTotalValue() > 0) tooltip += " (" + QuantityFormatter.quantityToStackSize(item.getTotalValue()) + " gp)";
        slot.setToolTipText(tooltip);
        return slot;
    }

    void clearRecords() {
        SwingUtilities.invokeLater(() -> {
            contentPanel.removeAll();
            collapsedState.clear();
            totalValueLabel.setText("Total value: 0 gp");
            totalCountLabel.setText("Total count: 0");
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    // WrapLayout: wraps items to next line
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override
        public Dimension minimumLayoutSize(Container target) { return layoutSize(target, false); }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                Insets ins = target.getInsets();
                int maxW = targetWidth - ins.left - ins.right - getHgap() * 2;
                Dimension dim = new Dimension(0, 0);
                int rowW = 0, rowH = 0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowW + d.width > maxW && rowW > 0) {
                        dim.width = Math.max(dim.width, rowW);
                        dim.height += rowH + getVgap();
                        rowW = 0; rowH = 0;
                    }
                    rowW += d.width + getHgap();
                    rowH = Math.max(rowH, d.height);
                }
                dim.width = Math.max(dim.width, rowW);
                dim.height += rowH + ins.top + ins.bottom + getVgap() * 2;
                return dim;
            }
        }
    }
}