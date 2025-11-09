package game.ui;

import java.awt.*;

/**
 * Vẽ HUD (score/lives/level) và các nút góc phải.
 * Dùng cùng với GamePanel: renderTopHUD(...) & renderDimOverlay(...).
 */
public final class HudOverlay {

    private final Font hudFont = new Font("Monospaced", Font.PLAIN, 14);
    private final Font titleFont = new Font("Monospaced", Font.PLAIN, 18);

    public HudOverlay() {}

    /** Vẽ dòng HUD phía trên + khung (nhẹ) cho các nút ở góc phải. */
    public void renderTopHUD(Graphics2D g2,
                             int score, int lives, int level,
                             Rectangle pauseBtn, Rectangle homeBtn, Rectangle settingsBtn) {
        g2.setFont(hudFont);
        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 12, 20);
        g2.drawString("Lives: " + Math.max(0, lives), 120, 20);
        g2.drawString("Level: " + level + "/5", 200, 20);

        // đường viền mảnh quanh các nút (icon được vẽ ở GamePanel)
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 100));
        g2.draw(pauseBtn);
        g2.draw(homeBtn);
        g2.draw(settingsBtn);
        g2.setStroke(old);
    }

    /**
     * Phủ nền mờ + tiêu đề trạng thái.
     * Nếu state là SETTINGS thì gọi SettingsOverlay.render(...)
     */
    public void renderDimOverlay(Graphics2D g2,
                                 String stateName,
                                 SettingsOverlay settings,
                                 boolean musicOn) {
        // phủ mờ
        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillRect(0, 0, g2.getDeviceConfiguration().getBounds().width,
                g2.getDeviceConfiguration().getBounds().height);

        // tiêu đề
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        String title = switch (stateName) {
            case "PAUSE" -> "PAUSED";
            case "SETTINGS" -> "SETTINGS";
            case "GAMEOVER" -> "GAME OVER - Press R to Retry";
            case "WIN" -> "LEVEL CLEARED - Press N for Next (or R to Restart)";
            default -> stateName;
        };

        if (title != null && !title.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics();
            int w = g2.getDeviceConfiguration().getBounds().width;
            int h = g2.getDeviceConfiguration().getBounds().height;
            int tw = fm.stringWidth(title);
            g2.drawString(title, (w - tw) / 2, h / 3);
        }

        // settings UI
        if ("SETTINGS".equals(stateName) && settings != null) {
            settings.render(g2, musicOn);
        }
    }
}
