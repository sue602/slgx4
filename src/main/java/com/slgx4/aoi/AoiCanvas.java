package com.slgx4.aoi;

import com.slgx4.aoi.algorithm.AoiHotPairSnapshot;
import com.slgx4.aoi.algorithm.AoiObjectSnapshot;
import com.slgx4.aoi.algorithm.AoiPosition;
import com.slgx4.aoi.algorithm.AoiSpace;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class AoiCanvas extends JPanel {
    interface Listener {
        void onSelect(Long id);

        void onAdd(AoiPosition position);

        void onMove(long id, AoiPosition position, boolean commit);
    }

    private static final int PADDING = 34;
    private static final Color BACKGROUND = new Color(14, 20, 29);
    private static final Color WORLD = new Color(28, 38, 50);
    private static final Color GRID = new Color(255, 255, 255, 24);
    private static final Color WATCHER = new Color(56, 189, 248);
    private static final Color MARKER = new Color(251, 146, 60);
    private static final Color BOTH = new Color(192, 132, 252);

    private final List<AoiDemoEntity> entities;
    private final List<AoiVisualEvent> visualEvents;
    private final Supplier<AoiSpace> spaceSupplier;
    private final Listener listener;
    private Long selectedId;
    private Long draggingId;
    private boolean addMode;

    AoiCanvas(List<AoiDemoEntity> entities, List<AoiVisualEvent> visualEvents,
              Supplier<AoiSpace> spaceSupplier, Listener listener) {
        this.entities = entities;
        this.visualEvents = visualEvents;
        this.spaceSupplier = spaceSupplier;
        this.listener = listener;
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(860, 680));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                AoiPosition worldPosition = toWorld(event.getX(), event.getY());
                if (worldPosition == null) {
                    return;
                }
                if (addMode) {
                    listener.onAdd(worldPosition);
                    setAddMode(false);
                    return;
                }
                Long hit = findEntity(worldPosition);
                selectedId = hit;
                draggingId = hit;
                listener.onSelect(hit);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (draggingId == null) {
                    return;
                }
                AoiPosition position = toWorld(event.getX(), event.getY());
                if (position != null) {
                    listener.onMove(draggingId, position, false);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (draggingId == null) {
                    return;
                }
                AoiPosition position = toWorld(event.getX(), event.getY());
                long id = draggingId;
                draggingId = null;
                if (position != null) {
                    listener.onMove(id, position, true);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setSelectedId(Long selectedId) {
        this.selectedId = selectedId;
        repaint();
    }

    void setAddMode(boolean addMode) {
        this.addMode = addMode;
        setCursor(Cursor.getPredefinedCursor(addMode ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Metrics metrics = metrics();

        drawWorld(g, metrics);
        drawRanges(g, metrics);
        drawHotPairs(g, metrics);
        drawEvents(g, metrics);
        drawEntities(g, metrics);
        drawLabels(g, metrics);
        g.dispose();
    }

    private void drawWorld(Graphics2D g, Metrics metrics) {
        g.setColor(WORLD);
        g.fillRoundRect(metrics.offsetX, metrics.offsetY, metrics.size, metrics.size, 12, 12);
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1));
        for (int value = 0; value <= 100; value += 10) {
            int pixel = worldToPixel(metrics, value);
            g.drawLine(pixel, metrics.offsetY, pixel, metrics.offsetY + metrics.size);
            g.drawLine(metrics.offsetX, pixel, metrics.offsetX + metrics.size, pixel);
        }
        g.setColor(new Color(148, 163, 184));
        g.setFont(getFont().deriveFont(10f));
        for (int value = 0; value <= 100; value += 20) {
            int x = worldToPixel(metrics, value);
            int y = worldToPixel(metrics, value);
            g.drawString(Integer.toString(value), x - 5, metrics.offsetY + metrics.size + 18);
            g.drawString(Integer.toString(value), metrics.offsetX - 26, y + 4);
        }
    }

    private void drawRanges(Graphics2D g, Metrics metrics) {
        Map<Long, AoiObjectSnapshot> snapshots = snapshotMap();
        for (AoiDemoEntity entity : entities) {
            AoiObjectSnapshot snapshot = snapshots.get(entity.id);
            if (!entity.watcher()) {
                continue;
            }
            int centerX = worldToPixel(metrics, entity.position.x());
            int centerY = worldToPixel(metrics, entity.position.y());
            double radius = AoiSpace.AOI_RADIUS / 100.0 * metrics.size;
            g.setColor(new Color(WATCHER.getRed(), WATCHER.getGreen(), WATCHER.getBlue(), 20));
            g.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2));
            g.setColor(new Color(WATCHER.getRed(), WATCHER.getGreen(), WATCHER.getBlue(), 105));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2));

            if (selectedId != null && selectedId == entity.id && snapshot != null) {
                drawSelectedThresholds(g, metrics, snapshot);
            }
        }
    }

    private void drawSelectedThresholds(Graphics2D g, Metrics metrics, AoiObjectSnapshot snapshot) {
        int keyX = worldToPixel(metrics, snapshot.lastKeyPosition().x());
        int keyY = worldToPixel(metrics, snapshot.lastKeyPosition().y());
        double shiftRadius = AoiSpace.SHIFT_RADIUS / 100.0 * metrics.size;
        g.setColor(new Color(74, 222, 128, 180));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10, new float[]{3, 4}, 0));
        g.draw(new Ellipse2D.Double(keyX - shiftRadius, keyY - shiftRadius,
                shiftRadius * 2, shiftRadius * 2));

        int currentX = worldToPixel(metrics, snapshot.position().x());
        int currentY = worldToPixel(metrics, snapshot.position().y());
        double hotRadius = AoiSpace.HOT_PAIR_RADIUS / 100.0 * metrics.size;
        g.setColor(new Color(250, 204, 21, 100));
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10, new float[]{8, 5}, 0));
        g.draw(new Ellipse2D.Double(currentX - hotRadius, currentY - hotRadius,
                hotRadius * 2, hotRadius * 2));
    }

    private void drawHotPairs(Graphics2D g, Metrics metrics) {
        Map<Long, AoiDemoEntity> byId = entityMap();
        g.setColor(new Color(250, 204, 21, 105));
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10, new float[]{5, 5}, 0));
        for (AoiHotPairSnapshot pair : spaceSupplier.get().hotPairs()) {
            AoiDemoEntity watcher = byId.get(pair.watcherId());
            AoiDemoEntity marker = byId.get(pair.markerId());
            if (watcher != null && marker != null) {
                g.drawLine(worldToPixel(metrics, watcher.position.x()), worldToPixel(metrics, watcher.position.y()),
                        worldToPixel(metrics, marker.position.x()), worldToPixel(metrics, marker.position.y()));
            }
        }
    }

    private void drawEvents(Graphics2D g, Metrics metrics) {
        Map<Long, AoiDemoEntity> byId = entityMap();
        for (AoiVisualEvent event : visualEvents) {
            AoiDemoEntity watcher = byId.get(event.watcherId);
            AoiDemoEntity marker = byId.get(event.markerId);
            if (watcher == null || marker == null) {
                continue;
            }
            int alpha = Math.max(35, Math.min(255, event.remainingFrames * 16));
            int x1 = worldToPixel(metrics, watcher.position.x());
            int y1 = worldToPixel(metrics, watcher.position.y());
            int x2 = worldToPixel(metrics, marker.position.x());
            int y2 = worldToPixel(metrics, marker.position.y());
            g.setColor(new Color(253, 224, 71, alpha));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x1, y1, x2, y2);
            drawArrowHead(g, x1, y1, x2, y2);
        }
    }

    private void drawEntities(Graphics2D g, Metrics metrics) {
        for (AoiDemoEntity entity : entities) {
            int x = worldToPixel(metrics, entity.position.x());
            int y = worldToPixel(metrics, entity.position.y());
            Color color = roleColor(entity);
            if (selectedId != null && selectedId == entity.id) {
                g.setColor(new Color(254, 240, 138, 105));
                g.fill(new Ellipse2D.Double(x - 15, y - 15, 30, 30));
            }
            if (entity.watcher() && entity.marker()) {
                Path2D hexagon = new Path2D.Double();
                for (int index = 0; index < 6; index++) {
                    double angle = Math.PI / 3 * index - Math.PI / 2;
                    double px = x + Math.cos(angle) * 10;
                    double py = y + Math.sin(angle) * 10;
                    if (index == 0) {
                        hexagon.moveTo(px, py);
                    } else {
                        hexagon.lineTo(px, py);
                    }
                }
                hexagon.closePath();
                g.setColor(new Color(15, 20, 29));
                g.fill(hexagon);
                g.setColor(color);
                g.setStroke(new BasicStroke(2.4f));
                g.draw(hexagon);
            } else if (entity.watcher()) {
                g.setColor(new Color(15, 20, 29));
                g.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
                g.setColor(color);
                g.setStroke(new BasicStroke(2.4f));
                g.draw(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
                g.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
            } else {
                Path2D diamond = new Path2D.Double();
                diamond.moveTo(x, y - 10);
                diamond.lineTo(x + 10, y);
                diamond.lineTo(x, y + 10);
                diamond.lineTo(x - 10, y);
                diamond.closePath();
                g.setColor(new Color(15, 20, 29));
                g.fill(diamond);
                g.setColor(color);
                g.setStroke(new BasicStroke(2.4f));
                g.draw(diamond);
            }
            drawVelocity(g, metrics, entity, color);
        }
    }

    private void drawLabels(Graphics2D g, Metrics metrics) {
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        for (AoiDemoEntity entity : entities) {
            int x = worldToPixel(metrics, entity.position.x());
            int y = worldToPixel(metrics, entity.position.y());
            String label = "#" + Long.toUnsignedString(entity.id) + "  " + entity.mode.toUpperCase();
            int width = g.getFontMetrics().stringWidth(label) + 8;
            g.setColor(new Color(7, 11, 17, 205));
            g.fillRoundRect(x + 11, y - 18, width, 18, 7, 7);
            g.setColor(roleColor(entity));
            g.drawString(label, x + 15, y - 5);
        }
        if (addMode) {
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(5, 9, 14, 220));
            g.fillRoundRect(metrics.offsetX + 12, metrics.offsetY + 12, 204, 30, 10, 10);
            g.setColor(new Color(254, 240, 138));
            g.drawString("新增模式：点击世界坐标", metrics.offsetX + 24, metrics.offsetY + 33);
        }
    }

    private void drawVelocity(Graphics2D g, Metrics metrics, AoiDemoEntity entity, Color color) {
        if (entity.velocityX == 0 && entity.velocityY == 0) {
            return;
        }
        int x = worldToPixel(metrics, entity.position.x());
        int y = worldToPixel(metrics, entity.position.y());
        double scale = metrics.size / 100.0 * 5;
        int x2 = x + (int) Math.round(entity.velocityX * scale);
        int y2 = y + (int) Math.round(entity.velocityY * scale);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 170));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(x, y, x2, y2);
        drawArrowHead(g, x, y, x2, y2);
    }

    private void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int size = 7;
        int firstX = x2 - (int) Math.round(Math.cos(angle - Math.PI / 6) * size);
        int firstY = y2 - (int) Math.round(Math.sin(angle - Math.PI / 6) * size);
        int secondX = x2 - (int) Math.round(Math.cos(angle + Math.PI / 6) * size);
        int secondY = y2 - (int) Math.round(Math.sin(angle + Math.PI / 6) * size);
        g.drawLine(x2, y2, firstX, firstY);
        g.drawLine(x2, y2, secondX, secondY);
    }

    private Long findEntity(AoiPosition position) {
        double thresholdSquared = 3.0 * 3.0;
        AoiDemoEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (AoiDemoEntity entity : entities) {
            double distance = entity.position.distanceSquared(position);
            if (distance <= thresholdSquared && distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest == null ? null : nearest.id;
    }

    private AoiPosition toWorld(int pixelX, int pixelY) {
        Metrics metrics = metrics();
        if (pixelX < metrics.offsetX || pixelX > metrics.offsetX + metrics.size
                || pixelY < metrics.offsetY || pixelY > metrics.offsetY + metrics.size) {
            return null;
        }
        float x = (pixelX - metrics.offsetX) * 100.0f / metrics.size;
        float y = (pixelY - metrics.offsetY) * 100.0f / metrics.size;
        return AoiPosition.of(x, y);
    }

    private int worldToPixel(Metrics metrics, float coordinate) {
        return metrics.offsetX + Math.round(coordinate / 100.0f * metrics.size);
    }

    private Map<Long, AoiDemoEntity> entityMap() {
        Map<Long, AoiDemoEntity> result = new HashMap<>();
        for (AoiDemoEntity entity : entities) {
            result.put(entity.id, entity);
        }
        return result;
    }

    private Map<Long, AoiObjectSnapshot> snapshotMap() {
        Map<Long, AoiObjectSnapshot> result = new HashMap<>();
        for (AoiObjectSnapshot snapshot : spaceSupplier.get().objects()) {
            result.put(snapshot.id(), snapshot);
        }
        return result;
    }

    private Color roleColor(AoiDemoEntity entity) {
        if (entity.watcher() && entity.marker()) {
            return BOTH;
        }
        return entity.watcher() ? WATCHER : MARKER;
    }

    private Metrics metrics() {
        int size = Math.max(100, Math.min(getWidth() - PADDING * 2, getHeight() - PADDING * 2));
        return new Metrics(size, (getWidth() - size) / 2, (getHeight() - size) / 2 - 4);
    }

    private record Metrics(int size, int offsetX, int offsetY) {
    }
}
