package com.slgx4.aoi;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.util.Enumeration;

public final class AOIDemoApplication {
    private AOIDemoApplication() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            installReadableFont();
            new AoiDemoFrame().setVisible(true);
        });
    }

    private static void installReadableFont() {
        Font font = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof Font) {
                UIManager.put(key, font);
            }
        }
    }
}
