package com.slgx4.territory.ui;

import com.slgx4.territory.algorithm.ConvexHull;
import com.slgx4.territory.algorithm.FloodFill;
import com.slgx4.territory.algorithm.TerritoryComponents;
import com.slgx4.territory.algorithm.VoronoiTerritory;
import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import com.slgx4.territory.model.Outpost;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class MainFrame extends JFrame {
    private static final Color WINDOW_BACKGROUND = new Color(13, 18, 25);
    private static final Color PANEL_BACKGROUND = new Color(24, 31, 41);
    private static final Color CARD_BACKGROUND = new Color(31, 40, 52);
    private static final Color TEXT = new Color(225, 232, 240);
    private static final Color MUTED_TEXT = new Color(154, 166, 181);
    private static final Color ACCENT = new Color(56, 189, 248);

    private final GridMap map = new GridMap(28, 18);
    private final List<Outpost> outposts = new ArrayList<>();
    private final Map<InteractionMode, JToggleButton> modeButtons = new EnumMap<>(InteractionMode.class);
    private final MapPanel mapPanel = new MapPanel(map, outposts);
    private final JComboBox<Faction> factionBox = new JComboBox<>(new Faction[]{Faction.BLUE, Faction.RED});
    private final JSpinner radiusSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
    private final JLabel modeHintLabel = new JLabel();
    private final JLabel statsLabel = new JLabel();
    private final JTextArea eventLog = new JTextArea();

    private InteractionMode mode = InteractionMode.BFS_EXPAND;
    private boolean interactionLocked;

    public MainFrame() {
        super("SLG 联盟领土算法实验室");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setSize(1340, 820);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WINDOW_BACKGROUND);
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(mapPanel, BorderLayout.CENTER);
        root.add(createSidebar(), BorderLayout.EAST);
        setContentPane(root);

        mapPanel.setCellClickHandler(this::handleMapClick);
        factionBox.addActionListener(event -> refresh("已切换当前联盟，可继续编辑地图。"));
        loadPreset();
        selectMode(InteractionMode.BFS_EXPAND);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(17, 23, 32));
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("SLG 联盟领土算法实验室");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subtitle = new JLabel("BFS · 连通性 · Union-Find · Marching Squares · Convex Hull · Voronoi");
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 7));
        legend.setOpaque(false);
        legend.add(legendItem(Faction.BLUE));
        legend.add(legendItem(Faction.RED));
        JLabel legendText = new JLabel("◆ 主城    △ 哨塔    ▲ 山脉");
        legendText.setForeground(MUTED_TEXT);
        legend.add(legendText);
        header.add(legend, BorderLayout.EAST);
        return header;
    }

    private JPanel legendItem(Faction faction) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(faction.lightColor());
        JLabel text = new JLabel(faction.displayName());
        text.setForeground(TEXT);
        item.add(dot);
        item.add(text);
        return item;
    }

    private JScrollPane createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL_BACKGROUND);
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        sidebar.setPreferredSize(new Dimension(330, 700));

        sidebar.add(sectionTitle("当前联盟与参数"));
        sidebar.add(Box.createVerticalStrut(9));
        JPanel parameters = new JPanel(new GridLayout(2, 2, 8, 8));
        parameters.setOpaque(false);
        parameters.add(fieldLabel("操作联盟"));
        styleComboBox(factionBox);
        parameters.add(factionBox);
        parameters.add(fieldLabel("BFS 半径"));
        styleSpinner(radiusSpinner);
        parameters.add(radiusSpinner);
        sidebar.add(parameters);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(sectionTitle("地图点击工具"));
        sidebar.add(Box.createVerticalStrut(9));
        JPanel modes = new JPanel(new GridLayout(3, 2, 8, 8));
        modes.setOpaque(false);
        ButtonGroup modeGroup = new ButtonGroup();
        for (InteractionMode candidate : InteractionMode.values()) {
            JToggleButton button = new JToggleButton(candidate.displayName());
            styleToggleButton(button);
            button.addActionListener(event -> selectMode(candidate));
            modeGroup.add(button);
            modeButtons.put(candidate, button);
            modes.add(button);
        }
        sidebar.add(modes);
        sidebar.add(Box.createVerticalStrut(8));
        modeHintLabel.setForeground(MUTED_TEXT);
        modeHintLabel.setFont(modeHintLabel.getFont().deriveFont(12f));
        modeHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(modeHintLabel);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(sectionTitle("算法操作"));
        sidebar.add(Box.createVerticalStrut(9));
        JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
        actions.setOpaque(false);
        JButton connectivity = actionButton("连通校验");
        connectivity.setToolTipText("BFS 检测并剥离无法连接到主城的格子");
        connectivity.addActionListener(event -> validateConnectivity());
        JButton hull = actionButton("凸包圈地");
        hull.setToolTipText("计算当前联盟哨塔的凸包并填充内部格子");
        hull.addActionListener(event -> fillConvexHull());
        JButton voronoi = actionButton("据点分区");
        voronoi.setToolTipText("按主城和哨塔的最近距离生成 Voronoi 势力区");
        voronoi.addActionListener(event -> applyVoronoi());
        JButton preset = actionButton("重置示例");
        preset.addActionListener(event -> loadPreset());
        actions.add(connectivity);
        actions.add(hull);
        actions.add(voronoi);
        actions.add(preset);
        sidebar.add(actions);

        sidebar.add(Box.createVerticalStrut(18));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(255, 255, 255, 30));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(separator);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(sectionTitle("实时数据（并查集）"));
        sidebar.add(Box.createVerticalStrut(8));
        statsLabel.setOpaque(true);
        statsLabel.setBackground(CARD_BACKGROUND);
        statsLabel.setForeground(TEXT);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        sidebar.add(statsLabel);

        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(sectionTitle("操作记录"));
        sidebar.add(Box.createVerticalStrut(8));
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        eventLog.setRows(5);
        eventLog.setBackground(CARD_BACKGROUND);
        eventLog.setForeground(new Color(197, 208, 220));
        eventLog.setCaretColor(TEXT);
        eventLog.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        eventLog.setFont(eventLog.getFont().deriveFont(12f));
        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScroll.setPreferredSize(new Dimension(290, 120));
        logScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        sidebar.add(logScroll);
        sidebar.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(sidebar);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private void handleMapClick(GridPoint point) {
        if (interactionLocked) {
            appendEvent("动画进行中，请稍候。", false);
            return;
        }
        Faction faction = selectedFaction();
        switch (mode) {
            case BFS_EXPAND -> animateBfs(point, faction);
            case OCCUPY -> occupyCell(point, faction);
            case CUT -> cutCell(point);
            case OBSTACLE -> toggleObstacle(point);
            case OUTPOST -> placeOutpost(point, faction);
            case CORE -> placeCore(point, faction);
        }
    }

    private void animateBfs(GridPoint start, Faction faction) {
        int radius = (int) radiusSpinner.getValue();
        Map<GridPoint, Integer> reachable = FloodFill.reachable(map, start, faction, radius);
        if (reachable.isEmpty()) {
            appendEvent("BFS 起点不可用：它可能是障碍或敌方领土。", true);
            return;
        }

        Map<Integer, List<GridPoint>> layers = new TreeMap<>();
        reachable.forEach((point, distance) -> layers.computeIfAbsent(distance, ignored -> new ArrayList<>()).add(point));
        List<Integer> distances = new ArrayList<>(layers.keySet());
        int[] index = {0};
        interactionLocked = true;
        Timer timer = new Timer(115, null);
        timer.addActionListener(event -> {
            int distance = distances.get(index[0]);
            for (GridPoint point : layers.get(distance)) {
                map.setOwner(point, faction);
            }
            mapPanel.setOverlayText("BFS 第 " + distance + " 层 · 已访问 "
                    + layers.values().stream().limit(index[0] + 1L).mapToInt(List::size).sum() + " 格");
            updateStats();
            mapPanel.repaint();
            index[0]++;
            if (index[0] >= distances.size()) {
                timer.stop();
                interactionLocked = false;
                mapPanel.setOverlayText(mode.displayName() + "｜" + mode.hint());
                appendEvent("BFS 扩张完成：半径 " + radius + "，占领/经过 " + reachable.size() + " 格。", true);
            }
        });
        timer.start();
    }

    private void occupyCell(GridPoint point, Faction faction) {
        if (map.isBlocked(point)) {
            appendEvent("障碍格不能被占领。", true);
            return;
        }
        Faction coreOwner = coreOwnerAt(point);
        if (coreOwner != Faction.NONE && coreOwner != faction) {
            appendEvent("不能直接覆盖敌方主城，请先重置或移动主城。", true);
            return;
        }
        map.setOwner(point, faction);
        refresh("已占领格子 " + coordinate(point) + "。并查集统计已实时更新。");
    }

    private void cutCell(GridPoint point) {
        if (coreOwnerAt(point) != Faction.NONE) {
            appendEvent("主城格不能被切断；可用“设置主城”移动它。", true);
            return;
        }
        Faction previous = map.ownerAt(point);
        map.setOwner(point, Faction.NONE);
        outposts.removeIf(outpost -> outpost.position().equals(point));
        refresh(previous == Faction.NONE
                ? "该格本来就是中立格。"
                : "已切断 " + coordinate(point) + "；点击“连通校验”观察失联领土剥离。");
    }

    private void toggleObstacle(GridPoint point) {
        if (coreOwnerAt(point) != Faction.NONE) {
            appendEvent("主城格不能改为障碍。", true);
            return;
        }
        boolean newValue = !map.isBlocked(point);
        map.setBlocked(point, newValue);
        if (newValue) {
            outposts.removeIf(outpost -> outpost.position().equals(point));
        }
        refresh(coordinate(point) + (newValue ? " 已设为山脉障碍。" : " 已恢复为可通行格。"));
    }

    private void placeOutpost(GridPoint point, Faction faction) {
        if (map.isBlocked(point)) {
            appendEvent("哨塔不能建在障碍格。", true);
            return;
        }
        if (map.ownerAt(point) != Faction.NONE && map.ownerAt(point) != faction) {
            appendEvent("哨塔不能直接建在敌方领土。", true);
            return;
        }
        if (coreOwnerAt(point) != Faction.NONE) {
            appendEvent("主城格无需重复放置哨塔。", true);
            return;
        }
        outposts.removeIf(outpost -> outpost.position().equals(point));
        outposts.add(new Outpost(point, faction));
        map.setOwner(point, faction);
        long count = outposts.stream().filter(outpost -> outpost.faction() == faction).count();
        refresh("已放置哨塔 " + coordinate(point) + "，当前联盟共有 " + count + " 座。虚线为实时凸包。");
    }

    private void placeCore(GridPoint point, Faction faction) {
        if (map.isBlocked(point)) {
            appendEvent("主城不能放在障碍格。", true);
            return;
        }
        Faction existingCore = coreOwnerAt(point);
        if (existingCore != Faction.NONE && existingCore != faction) {
            appendEvent("这里已有敌方主城。", true);
            return;
        }
        map.setCore(faction, point);
        refresh("已将 " + faction.displayName() + " 主城设在 " + coordinate(point) + "。");
    }

    private void validateConnectivity() {
        Faction faction = selectedFaction();
        if (map.coreOf(faction) == null) {
            appendEvent("当前联盟还没有主城，请先用“设置主城”。", true);
            return;
        }
        Set<GridPoint> removed = FloodFill.removeDisconnected(map, faction);
        outposts.removeIf(outpost -> outpost.faction() == faction && removed.contains(outpost.position()));
        refresh(removed.isEmpty()
                ? "连通校验完成：所有领土都能通过四连通路径回到主城。"
                : "连通校验完成：剥离了 " + removed.size() + " 个失联格子。");
    }

    private void fillConvexHull() {
        Faction faction = selectedFaction();
        List<GridPoint> posts = outposts.stream()
                .filter(outpost -> outpost.faction() == faction)
                .map(Outpost::position)
                .toList();
        if (posts.size() < 3) {
            appendEvent("凸包圈地至少需要 3 座当前联盟的哨塔。", true);
            return;
        }
        int hullVertices = ConvexHull.compute(posts).size();
        Set<GridPoint> filled = ConvexHull.fillTerritory(map, posts, faction);
        refresh("凸包圈地完成：" + hullVertices + " 个边界顶点，包围/保留 " + filled.size() + " 格。敌方与障碍未覆盖。");
    }

    private void applyVoronoi() {
        List<Outpost> sites = new ArrayList<>(outposts);
        for (Faction faction : List.of(Faction.BLUE, Faction.RED)) {
            GridPoint core = map.coreOf(faction);
            if (core != null) {
                sites.add(new Outpost(core, faction));
            }
        }
        if (sites.size() < 2) {
            appendEvent("据点分区至少需要两个主城或哨塔。", true);
            return;
        }
        VoronoiTerritory.assign(map, sites);
        refresh("Voronoi 据点分区完成：每格归属最近据点，异联盟等距格保持中立。");
    }

    private void loadPreset() {
        if (interactionLocked) {
            return;
        }
        map.clear();
        outposts.clear();

        for (int y = 2; y <= 15; y++) {
            if (y != 5 && y != 12) {
                map.setBlocked(new GridPoint(13, y), true);
            }
        }
        for (GridPoint point : List.of(
                new GridPoint(2, 3), new GridPoint(3, 3), new GridPoint(4, 3),
                new GridPoint(18, 14), new GridPoint(19, 14), new GridPoint(20, 14))) {
            map.setBlocked(point, true);
        }

        map.setCore(Faction.BLUE, new GridPoint(5, 9));
        map.setCore(Faction.RED, new GridPoint(22, 8));
        FloodFill.expand(map, new GridPoint(5, 9), Faction.BLUE, 4);
        FloodFill.expand(map, new GridPoint(22, 8), Faction.RED, 4);

        // 一小块故意失联的蓝方领土，用于展示并查集组件统计和连通剥离。
        map.setOwner(new GridPoint(10, 2), Faction.BLUE);
        map.setOwner(new GridPoint(10, 3), Faction.BLUE);

        addPresetOutpost(new GridPoint(3, 11), Faction.BLUE);
        addPresetOutpost(new GridPoint(5, 13), Faction.BLUE);
        addPresetOutpost(new GridPoint(9, 9), Faction.BLUE);
        addPresetOutpost(new GridPoint(20, 5), Faction.RED);
        addPresetOutpost(new GridPoint(25, 5), Faction.RED);
        addPresetOutpost(new GridPoint(24, 11), Faction.RED);

        refresh("示例已重置。蓝方含一块失联领土，可直接点击“连通校验”观察剥离。", false);
    }

    private void addPresetOutpost(GridPoint point, Faction faction) {
        outposts.add(new Outpost(point, faction));
        map.setOwner(point, faction);
    }

    private void selectMode(InteractionMode selected) {
        mode = selected;
        JToggleButton button = modeButtons.get(selected);
        if (button != null) {
            button.setSelected(true);
        }
        modeHintLabel.setText(selected.hint());
        mapPanel.setOverlayText(selected.displayName() + "｜" + selected.hint());
    }

    private void refresh(String message) {
        refresh(message, true);
    }

    private void refresh(String message, boolean append) {
        updateStats();
        mapPanel.repaint();
        appendEvent(message, append);
    }

    private void updateStats() {
        Faction faction = selectedFaction();
        TerritoryComponents.Summary summary = TerritoryComponents.analyze(map, faction);
        statsLabel.setText("<html><b>" + faction.displayName() + "</b>　领土 " + summary.occupiedCellCount()
                + " 格<br>连通块 " + summary.componentCount() + "　最大块 " + summary.largestComponentSize() + " 格</html>");
    }

    private void appendEvent(String message, boolean append) {
        if (!append) {
            eventLog.setText("• " + message);
        } else {
            String oldText = eventLog.getText();
            eventLog.setText("• " + message + (oldText.isBlank() ? "" : "\n\n" + oldText));
        }
        eventLog.setCaretPosition(0);
    }

    private Faction selectedFaction() {
        Faction selected = (Faction) factionBox.getSelectedItem();
        return selected == null ? Faction.BLUE : selected;
    }

    private Faction coreOwnerAt(GridPoint point) {
        for (Faction faction : List.of(Faction.BLUE, Faction.RED)) {
            if (point.equals(map.coreOf(faction))) {
                return faction;
            }
        }
        return Faction.NONE;
    }

    private static String coordinate(GridPoint point) {
        return "(" + point.x() + ", " + point.y() + ")";
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(MUTED_TEXT);
        return label;
    }

    private JButton actionButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(new Color(44, 56, 71));
        button.setForeground(TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 28)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        return button;
    }

    private void styleToggleButton(JToggleButton button) {
        button.setFocusPainted(false);
        button.setBackground(new Color(38, 48, 61));
        button.setForeground(TEXT);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 25)),
                BorderFactory.createEmptyBorder(7, 5, 7, 5)));
        button.addChangeListener(event -> {
            boolean selected = button.isSelected();
            button.setBackground(selected ? new Color(37, 99, 160) : new Color(38, 48, 61));
            button.setForeground(selected ? Color.WHITE : TEXT);
        });
    }

    private void styleComboBox(JComboBox<Faction> comboBox) {
        comboBox.setBackground(CARD_BACKGROUND);
        comboBox.setForeground(TEXT);
        comboBox.setFocusable(false);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 30)));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(CARD_BACKGROUND);
        editor.getTextField().setForeground(TEXT);
        editor.getTextField().setCaretColor(ACCENT);
    }
}
