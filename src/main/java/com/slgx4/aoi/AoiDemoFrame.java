package com.slgx4.aoi;

import com.slgx4.aoi.algorithm.AoiEvent;
import com.slgx4.aoi.algorithm.AoiPosition;
import com.slgx4.aoi.algorithm.AoiSpace;
import com.slgx4.aoi.algorithm.AoiStats;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
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
import java.util.List;

final class AoiDemoFrame extends JFrame {
    private static final Color WINDOW = new Color(11, 16, 23);
    private static final Color PANEL = new Color(23, 31, 42);
    private static final Color CARD = new Color(31, 42, 55);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color CYAN = new Color(56, 189, 248);
    private static final Color ORANGE = new Color(251, 146, 60);
    private static final Color PURPLE = new Color(192, 132, 252);

    private final List<AoiDemoEntity> entities = new ArrayList<>();
    private final List<AoiVisualEvent> visualEvents = new ArrayList<>();
    private final List<String> eventLines = new ArrayList<>();
    private final AoiCanvas canvas;
    private final Timer timer;
    private final JToggleButton playButton = new JToggleButton("▶ 播放");
    private final JToggleButton addButton = new JToggleButton("＋ 地图点选新增");
    private final JComboBox<DemoRole> roleBox = new JComboBox<>(DemoRole.values());
    private final JLabel tickLabel = new JLabel();
    private final JLabel statsLabel = new JLabel();
    private final JLabel selectedLabel = new JLabel();
    private final JTextArea eventLog = new JTextArea();

    private AoiSpace space = AoiSpace.create();
    private Long selectedId;
    private int tick;
    private int totalEvents;
    private long nextObjectId = 10;

    AoiDemoFrame() {
        super("AOI 热点对算法 Java 复刻演示");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 740));
        setSize(1360, 840);
        setLocationRelativeTo(null);

        canvas = new AoiCanvas(entities, visualEvents, () -> space, new AoiCanvas.Listener() {
            @Override
            public void onSelect(Long id) {
                selectObject(id);
            }

            @Override
            public void onAdd(AoiPosition position) {
                addObject(position);
            }

            @Override
            public void onMove(long id, AoiPosition position, boolean commit) {
                moveObject(id, position, commit);
            }
        });
        timer = new Timer(135, event -> stepSimulation());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WINDOW);
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(canvas, BorderLayout.CENTER);
        root.add(createSidebar(), BorderLayout.EAST);
        setContentPane(root);

        resetScenario();
    }

    @Override
    public void dispose() {
        timer.stop();
        space.close();
        super.dispose();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(16, 23, 33));
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JPanel titleBox = transparentPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("AOI 动态热点对实验室");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subtitle = new JLabel("cloudwu/aoi Java 语义移植 · 固定 R=10 · 3D 欧氏距离 · 仅进入事件");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        legend.setOpaque(false);
        legend.add(legend("●", "Watcher", CYAN));
        legend.add(legend("◆", "Marker", ORANGE));
        legend.add(legend("⬡", "Watcher + Marker", PURPLE));
        JLabel ranges = new JLabel("绿虚线 R/2　蓝圈 R　黄虚线 2R");
        ranges.setForeground(MUTED);
        legend.add(ranges);
        header.add(legend, BorderLayout.EAST);
        return header;
    }

    private JScrollPane createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL);
        sidebar.setBorder(BorderFactory.createEmptyBorder(17, 18, 17, 18));
        sidebar.setPreferredSize(new Dimension(350, 780));

        sidebar.add(sectionTitle("参考 test.c 场景"));
        sidebar.add(Box.createVerticalStrut(8));
        tickLabel.setOpaque(true);
        tickLabel.setBackground(CARD);
        tickLabel.setForeground(TEXT);
        tickLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        tickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tickLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        sidebar.add(tickLabel);
        sidebar.add(Box.createVerticalStrut(8));

        JPanel playback = new JPanel(new GridLayout(1, 3, 8, 8));
        playback.setOpaque(false);
        JButton step = button("单步 Tick");
        JButton reset = button("重置");
        styleToggle(playButton);
        playButton.addActionListener(event -> setRunning(playButton.isSelected()));
        step.addActionListener(event -> {
            setRunning(false);
            stepSimulation();
        });
        reset.addActionListener(event -> resetScenario());
        playback.add(playButton);
        playback.add(step);
        playback.add(reset);
        playback.setAlignmentX(Component.LEFT_ALIGNMENT);
        playback.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sidebar.add(playback);

        sidebar.add(Box.createVerticalStrut(9));
        JPanel speedRow = new JPanel(new BorderLayout(8, 0));
        speedRow.setOpaque(false);
        JLabel speedText = new JLabel("播放速度");
        speedText.setForeground(MUTED);
        JSlider speed = new JSlider(1, 8, 4);
        speed.setOpaque(false);
        speed.addChangeListener(event -> timer.setDelay(270 - speed.getValue() * 27));
        speedRow.add(speedText, BorderLayout.WEST);
        speedRow.add(speed, BorderLayout.CENTER);
        speedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        sidebar.add(speedRow);

        sidebar.add(Box.createVerticalStrut(16));
        sidebar.add(sectionTitle("AOI 内部状态"));
        sidebar.add(Box.createVerticalStrut(8));
        statsLabel.setOpaque(true);
        statsLabel.setBackground(CARD);
        statsLabel.setForeground(TEXT);
        statsLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        sidebar.add(statsLabel);

        sidebar.add(Box.createVerticalStrut(16));
        sidebar.add(sectionTitle("对象交互"));
        sidebar.add(Box.createVerticalStrut(8));
        selectedLabel.setOpaque(true);
        selectedLabel.setBackground(CARD);
        selectedLabel.setForeground(TEXT);
        selectedLabel.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11));
        selectedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        sidebar.add(selectedLabel);
        sidebar.add(Box.createVerticalStrut(8));

        JPanel roleRow = new JPanel(new GridLayout(1, 2, 8, 8));
        roleRow.setOpaque(false);
        roleBox.setBackground(CARD);
        roleBox.setForeground(TEXT);
        roleBox.setFocusable(false);
        styleToggle(addButton);
        addButton.addActionListener(event -> {
            setRunning(false);
            canvas.setAddMode(addButton.isSelected());
        });
        roleRow.add(roleBox);
        roleRow.add(addButton);
        roleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sidebar.add(roleRow);

        sidebar.add(Box.createVerticalStrut(8));
        JPanel objectActions = new JPanel(new GridLayout(1, 2, 8, 8));
        objectActions.setOpaque(false);
        JButton applyMode = button("应用模式到所选");
        JButton drop = button("Drop 所选");
        applyMode.addActionListener(event -> applyModeToSelected());
        drop.addActionListener(event -> dropSelected());
        objectActions.add(applyMode);
        objectActions.add(drop);
        objectActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        objectActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sidebar.add(objectActions);

        JLabel hint = new JLabel("点击对象查看；拖动对象会调用 update + message");
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(hint);

        sidebar.add(Box.createVerticalStrut(16));
        sidebar.add(sectionTitle("AOI 消息（Watcher → Marker）"));
        sidebar.add(Box.createVerticalStrut(8));
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        eventLog.setBackground(CARD);
        eventLog.setForeground(new Color(254, 240, 138));
        eventLog.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        eventLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane eventScroll = new JScrollPane(eventLog);
        eventScroll.setBorder(BorderFactory.createEmptyBorder());
        eventScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        eventScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        eventScroll.setPreferredSize(new Dimension(310, 160));
        eventScroll.setMinimumSize(new Dimension(310, 105));
        eventScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        sidebar.add(eventScroll);

        sidebar.add(Box.createVerticalStrut(12));
        JTextArea note = new JTextArea("参考场景：#0/#2 为 Watcher，#1/#3 同时是 Watcher 与 Marker。"
                + "对象每 tick 移动 1 单位；tick 50 按原 test.c Drop #3。算法不发送离开事件。");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(11f));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        sidebar.add(note);
        sidebar.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(sidebar);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private void stepSimulation() {
        if (tick >= 100) {
            setRunning(false);
            addLog("参考场景完成 100 tick；点击重置可再次运行。", false);
            return;
        }
        fadeVisualEvents();
        if (tick < 50) {
            for (AoiDemoEntity entity : entities) {
                entity.advance();
                space.update(entity.id, entity.mode, entity.position);
            }
        } else if (tick == 50) {
            AoiDemoEntity dropped = findEntity(3);
            if (dropped != null) {
                space.update(dropped.id, "d", dropped.position);
                entities.remove(dropped);
                if (selectedId != null && selectedId == 3) {
                    selectObject(null);
                }
                addLog("tick 50  DROP #3（与原 test.c 一致）", true);
            }
        } else {
            for (AoiDemoEntity entity : entities) {
                entity.advance();
                space.update(entity.id, entity.mode, entity.position);
            }
        }

        List<AoiEvent> events = space.message();
        recordEvents(events, "tick " + tick);
        tick++;
        refreshView();
        if (tick >= 100) {
            setRunning(false);
        }
    }

    private void resetScenario() {
        setRunning(false);
        space.close();
        space = AoiSpace.create();
        entities.clear();
        visualEvents.clear();
        eventLines.clear();
        tick = 0;
        totalEvents = 0;
        nextObjectId = 10;
        selectedId = null;
        canvas.setSelectedId(null);
        addButton.setSelected(false);
        canvas.setAddMode(false);

        // 与 cloudwu/aoi test.c 的 init_obj 参数逐项一致。
        entities.add(new AoiDemoEntity(0, "w", AoiPosition.of(40, 0), 0, 1));
        entities.add(new AoiDemoEntity(1, "wm", AoiPosition.of(42, 100), 0, -1));
        entities.add(new AoiDemoEntity(2, "w", AoiPosition.of(0, 40), 1, 0));
        entities.add(new AoiDemoEntity(3, "wm", AoiPosition.of(100, 45), -1, 0));
        addLog("场景已重置；点击播放或单步 Tick。", false);
        refreshView();
    }

    private void setRunning(boolean running) {
        playButton.setSelected(running);
        playButton.setText(running ? "Ⅱ 暂停" : "▶ 播放");
        if (running) {
            addButton.setSelected(false);
            canvas.setAddMode(false);
            timer.start();
        } else {
            timer.stop();
        }
    }

    private void selectObject(Long id) {
        selectedId = id;
        canvas.setSelectedId(id);
        updateSelectedLabel();
    }

    private void addObject(AoiPosition position) {
        setRunning(false);
        DemoRole role = selectedRole();
        AoiDemoEntity entity = new AoiDemoEntity(nextObjectId++, role.mode, position, 0, 0);
        entities.add(entity);
        space.update(entity.id, entity.mode, entity.position);
        recordEvents(space.message(), "手动新增");
        addButton.setSelected(false);
        canvas.setAddMode(false);
        selectObject(entity.id);
        addLog("新增 #" + entity.id + " mode=" + entity.mode + " @ " + format(position), true);
        refreshView();
    }

    private void moveObject(long id, AoiPosition position, boolean commit) {
        AoiDemoEntity entity = findEntity(id);
        if (entity == null) {
            return;
        }
        setRunning(false);
        entity.position = position;
        if (commit) {
            fadeVisualEvents();
            space.update(entity.id, entity.mode, entity.position);
            recordEvents(space.message(), "手动拖动");
            addLog("拖动 #" + id + " 到 " + format(position), true);
        }
        refreshView();
    }

    private void applyModeToSelected() {
        AoiDemoEntity entity = selectedId == null ? null : findEntity(selectedId);
        if (entity == null) {
            addLog("请先点击选择一个对象。", true);
            return;
        }
        setRunning(false);
        entity.mode = selectedRole().mode;
        space.update(entity.id, entity.mode, entity.position);
        recordEvents(space.message(), "模式变化");
        addLog("#" + entity.id + " 模式已改为 " + entity.mode, true);
        refreshView();
    }

    private void dropSelected() {
        AoiDemoEntity entity = selectedId == null ? null : findEntity(selectedId);
        if (entity == null) {
            addLog("请先点击选择一个对象。", true);
            return;
        }
        setRunning(false);
        space.update(entity.id, "d", entity.position);
        recordEvents(space.message(), "手动 Drop");
        entities.remove(entity);
        addLog("已 Drop #" + entity.id, true);
        selectObject(null);
        refreshView();
    }

    private void recordEvents(List<AoiEvent> events, String source) {
        for (AoiEvent event : events) {
            totalEvents++;
            visualEvents.add(new AoiVisualEvent(event.watcherId(), event.markerId()));
            addLog(source + "  #" + event.watcherId() + " → #" + event.markerId(), true);
        }
    }

    private void fadeVisualEvents() {
        for (AoiVisualEvent event : visualEvents) {
            event.remainingFrames--;
        }
        visualEvents.removeIf(event -> event.remainingFrames <= 0);
    }

    private void refreshView() {
        AoiStats stats = space.stats();
        tickLabel.setText("<html><b>Tick " + tick + " / 100</b>　累计消息 " + totalEvents
                + "<br>固定 AOI 半径 R = " + (int) AoiSpace.AOI_RADIUS + "</html>");
        statsLabel.setText("<html>对象 <b>" + stats.objectCount() + "</b>　热点对 <b>"
                + stats.hotPairCount() + "</b><br>Watcher：静 " + stats.watcherStaticCount()
                + " / 动 " + stats.watcherMoveCount() + "　 Marker：静 "
                + stats.markerStaticCount() + " / 动 " + stats.markerMoveCount() + "</html>");
        updateSelectedLabel();
        eventLog.setText(String.join("\n", eventLines));
        eventLog.setCaretPosition(0);
        canvas.repaint();
    }

    private void updateSelectedLabel() {
        AoiDemoEntity entity = selectedId == null ? null : findEntity(selectedId);
        if (entity == null) {
            selectedLabel.setText("<html><b>未选择对象</b><br>点击对象可查看关键点和拖动</html>");
            return;
        }
        String key = space.object(entity.id)
                .map(snapshot -> format(snapshot.lastKeyPosition()) + " / v" + snapshot.version())
                .orElse("尚未提交到 AOI");
        selectedLabel.setText("<html><b>#" + entity.id + "　mode=" + entity.mode + "</b>　当前位置 "
                + format(entity.position) + "<br>关键点 " + key + "</html>");
    }

    private void addLog(String line, boolean prepend) {
        if (!prepend) {
            eventLines.clear();
        }
        eventLines.add(0, line);
        if (eventLines.size() > 100) {
            eventLines.remove(eventLines.size() - 1);
        }
    }

    private AoiDemoEntity findEntity(long id) {
        return entities.stream().filter(entity -> entity.id == id).findFirst().orElse(null);
    }

    private DemoRole selectedRole() {
        DemoRole selected = (DemoRole) roleBox.getSelectedItem();
        return selected == null ? DemoRole.BOTH : selected;
    }

    private JPanel legend(String symbol, String text, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);
        JLabel icon = new JLabel(symbol);
        icon.setForeground(color);
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        item.add(icon);
        item.add(label);
        return item;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(new Color(42, 55, 70));
        button.setForeground(TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 27)),
                BorderFactory.createEmptyBorder(7, 7, 7, 7)));
        return button;
    }

    private void styleToggle(JToggleButton button) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(new Color(42, 55, 70));
        button.setForeground(TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 27)),
                BorderFactory.createEmptyBorder(7, 7, 7, 7)));
        button.addChangeListener(event -> {
            button.setBackground(button.isSelected() ? new Color(30, 105, 150) : new Color(42, 55, 70));
            button.setForeground(Color.WHITE);
        });
    }

    private JPanel transparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static String format(AoiPosition position) {
        return String.format("(%.1f, %.1f, %.1f)", position.x(), position.y(), position.z());
    }

    private enum DemoRole {
        WATCHER("观察者 w", "w"),
        MARKER("标记者 m", "m"),
        BOTH("双角色 wm", "wm");

        private final String label;
        private final String mode;

        DemoRole(String label, String mode) {
            this.label = label;
            this.mode = mode;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
