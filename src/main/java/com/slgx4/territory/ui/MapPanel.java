package com.slgx4.territory.ui;

import com.slgx4.territory.algorithm.ConvexHull;
import com.slgx4.territory.algorithm.LineSegment;
import com.slgx4.territory.algorithm.MarchingSquares;
import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import com.slgx4.territory.model.Outpost;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Consumer;

public final class MapPanel extends JPanel {
    private static final int PADDING = 28;
    private static final Color BACKGROUND = new Color(18, 24, 33);
    private static final Color CELL_A = new Color(35, 45, 57);
    private static final Color CELL_B = new Color(39, 50, 63);
    private static final Color GRID = new Color(255, 255, 255, 22);

    private final GridMap map;
    private final List<Outpost> outposts;
    private Consumer<GridPoint> cellClickHandler = point -> { };
    private GridPoint hovered;
    private String overlayText = "";

    public MapPanel(GridMap map, List<Outpost> outposts) {
        this.map = map;
        this.outposts = outposts;
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(920, 620));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                hovered = pointAt(event.getX(), event.getY());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                GridPoint point = pointAt(event.getX(), event.getY());
                if (point != null) {
                    cellClickHandler.accept(point);
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setCellClickHandler(Consumer<GridPoint> handler) {
        this.cellClickHandler = handler;
    }

    public void setOverlayText(String overlayText) {
        this.overlayText = overlayText == null ? "" : overlayText;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Metrics metrics = metrics();

        drawBaseCells(g, metrics);
        drawTerritory(g, metrics);
        drawHullOverlays(g, metrics);
        drawGrid(g, metrics);
        drawMarchingSquares(g, metrics);
        drawStructures(g, metrics);
        drawHover(g, metrics);
        drawOverlay(g);
        g.dispose();
    }

    private void drawBaseCells(Graphics2D g, Metrics metrics) {
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                GridPoint point = new GridPoint(x, y);
                int pixelX = metrics.offsetX + x * metrics.cellSize;
                int pixelY = metrics.offsetY + y * metrics.cellSize;
                g.setColor((x + y) % 2 == 0 ? CELL_A : CELL_B);
                g.fillRect(pixelX, pixelY, metrics.cellSize, metrics.cellSize);
                if (map.isBlocked(point)) {
                    drawMountain(g, pixelX, pixelY, metrics.cellSize);
                }
            }
        }
    }

    private void drawTerritory(Graphics2D g, Metrics metrics) {
        for (GridPoint point : map.points()) {
            Faction owner = map.ownerAt(point);
            if (owner == Faction.NONE || map.isBlocked(point)) {
                continue;
            }
            int x = metrics.offsetX + point.x() * metrics.cellSize;
            int y = metrics.offsetY + point.y() * metrics.cellSize;
            g.setColor(withAlpha(owner.color(), 108));
            g.fillRect(x + 1, y + 1, metrics.cellSize - 1, metrics.cellSize - 1);
        }
    }

    private void drawHullOverlays(Graphics2D g, Metrics metrics) {
        for (Faction faction : List.of(Faction.BLUE, Faction.RED)) {
            List<GridPoint> posts = outposts.stream()
                    .filter(outpost -> outpost.faction() == faction)
                    .map(Outpost::position)
                    .toList();
            List<GridPoint> hull = ConvexHull.compute(posts);
            if (hull.size() < 3) {
                continue;
            }
            Polygon polygon = new Polygon();
            for (GridPoint point : hull) {
                polygon.addPoint(centerX(metrics, point), centerY(metrics, point));
            }
            g.setColor(withAlpha(faction.color(), 30));
            g.fillPolygon(polygon);
            g.setColor(withAlpha(faction.lightColor(), 190));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10, new float[]{7, 5}, 0));
            g.drawPolygon(polygon);
        }
    }

    private void drawGrid(Graphics2D g, Metrics metrics) {
        g.setStroke(new BasicStroke(1));
        g.setColor(GRID);
        for (int x = 0; x <= map.width(); x++) {
            int pixelX = metrics.offsetX + x * metrics.cellSize;
            g.drawLine(pixelX, metrics.offsetY, pixelX, metrics.offsetY + map.height() * metrics.cellSize);
        }
        for (int y = 0; y <= map.height(); y++) {
            int pixelY = metrics.offsetY + y * metrics.cellSize;
            g.drawLine(metrics.offsetX, pixelY, metrics.offsetX + map.width() * metrics.cellSize, pixelY);
        }
    }

    private void drawMarchingSquares(Graphics2D g, Metrics metrics) {
        for (Faction faction : List.of(Faction.BLUE, Faction.RED)) {
            List<LineSegment> segments = MarchingSquares.extract(map, faction);
            g.setColor(faction.lightColor());
            g.setStroke(new BasicStroke(Math.max(2.2f, metrics.cellSize / 9f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (LineSegment segment : segments) {
                int x1 = metrics.offsetX + (int) Math.round(segment.x1() * metrics.cellSize);
                int y1 = metrics.offsetY + (int) Math.round(segment.y1() * metrics.cellSize);
                int x2 = metrics.offsetX + (int) Math.round(segment.x2() * metrics.cellSize);
                int y2 = metrics.offsetY + (int) Math.round(segment.y2() * metrics.cellSize);
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private void drawStructures(Graphics2D g, Metrics metrics) {
        for (Faction faction : List.of(Faction.BLUE, Faction.RED)) {
            GridPoint core = map.coreOf(faction);
            if (core != null) {
                drawCore(g, metrics, core, faction);
            }
        }
        for (Outpost outpost : outposts) {
            drawOutpost(g, metrics, outpost);
        }
    }

    private void drawCore(Graphics2D g, Metrics metrics, GridPoint point, Faction faction) {
        int centerX = centerX(metrics, point);
        int centerY = centerY(metrics, point);
        int radius = Math.max(6, metrics.cellSize / 3);
        g.setColor(new Color(14, 18, 25));
        g.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0));
        g.setColor(faction.lightColor());
        g.setStroke(new BasicStroke(2));
        g.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0));
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, metrics.cellSize * 0.42f)));
        g.drawString("◆", centerX - g.getFontMetrics().stringWidth("◆") / 2,
                centerY + g.getFontMetrics().getAscent() / 3);
    }

    private void drawOutpost(Graphics2D g, Metrics metrics, Outpost outpost) {
        int centerX = centerX(metrics, outpost.position());
        int centerY = centerY(metrics, outpost.position());
        int radius = Math.max(5, metrics.cellSize / 4);
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(centerX, centerY - radius);
        triangle.lineTo(centerX + radius, centerY + radius);
        triangle.lineTo(centerX - radius, centerY + radius);
        triangle.closePath();
        g.setColor(new Color(18, 24, 33));
        g.fill(triangle);
        g.setColor(outpost.faction().lightColor());
        g.setStroke(new BasicStroke(2));
        g.draw(triangle);
    }

    private void drawMountain(Graphics2D g, int x, int y, int size) {
        int padding = Math.max(3, size / 7);
        Path2D mountain = new Path2D.Double();
        mountain.moveTo(x + padding, y + size - padding);
        mountain.lineTo(x + size * 0.44, y + padding);
        mountain.lineTo(x + size * 0.62, y + size * 0.48);
        mountain.lineTo(x + size * 0.73, y + size * 0.28);
        mountain.lineTo(x + size - padding, y + size - padding);
        mountain.closePath();
        g.setColor(new Color(15, 20, 28));
        g.fill(mountain);
        g.setColor(new Color(120, 133, 151));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(mountain);
    }

    private void drawHover(Graphics2D g, Metrics metrics) {
        if (hovered == null) {
            return;
        }
        int x = metrics.offsetX + hovered.x() * metrics.cellSize;
        int y = metrics.offsetY + hovered.y() * metrics.cellSize;
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(255, 255, 255, 52));
        g.fillRect(x + 1, y + 1, metrics.cellSize - 1, metrics.cellSize - 1);
        g.setColor(new Color(255, 255, 255, 190));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(x + 1, y + 1, metrics.cellSize - 2, metrics.cellSize - 2);
    }

    private void drawOverlay(Graphics2D g) {
        if (overlayText.isBlank()) {
            return;
        }
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        int width = g.getFontMetrics().stringWidth(overlayText) + 24;
        g.setColor(new Color(5, 9, 14, 205));
        g.fillRoundRect(16, 14, width, 30, 12, 12);
        g.setColor(new Color(220, 229, 240));
        g.drawString(overlayText, 28, 34);
    }

    private GridPoint pointAt(int mouseX, int mouseY) {
        Metrics metrics = metrics();
        int x = (mouseX - metrics.offsetX) / metrics.cellSize;
        int y = (mouseY - metrics.offsetY) / metrics.cellSize;
        GridPoint point = new GridPoint(x, y);
        if (mouseX < metrics.offsetX || mouseY < metrics.offsetY || !map.contains(point)) {
            return null;
        }
        return point;
    }

    private Metrics metrics() {
        int availableWidth = Math.max(1, getWidth() - PADDING * 2);
        int availableHeight = Math.max(1, getHeight() - PADDING * 2);
        int cellSize = Math.max(4, Math.min(availableWidth / map.width(), availableHeight / map.height()));
        int mapWidth = cellSize * map.width();
        int mapHeight = cellSize * map.height();
        return new Metrics(cellSize, (getWidth() - mapWidth) / 2, (getHeight() - mapHeight) / 2);
    }

    private int centerX(Metrics metrics, GridPoint point) {
        return metrics.offsetX + point.x() * metrics.cellSize + metrics.cellSize / 2;
    }

    private int centerY(Metrics metrics, GridPoint point) {
        return metrics.offsetY + point.y() * metrics.cellSize + metrics.cellSize / 2;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private record Metrics(int cellSize, int offsetX, int offsetY) {
    }
}
