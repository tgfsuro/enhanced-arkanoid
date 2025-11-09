package game.ui;

import java.awt.*;

/**
 * Overlay cho màn Settings: Music toggle / Main Menu / Back
 * GamePanel gọi:
 *  - layoutCenter(w, h) một lần trong ctor
 *  - render(g2, musicOn) khi state == SETTINGS
 *  - hitToggle/ hitMainMenu/ hitBack trong xử lý click
 */
public final class SettingsOverlay {

    private final Font btnFont = new Font("Monospaced", Font.PLAIN, 14);
    private Rectangle btnMusicToggle = new Rectangle();
    private Rectangle btnMainMenu    = new Rectangle();
    private Rectangle btnBack        = new Rectangle();

    public SettingsOverlay() {}

    /** Tính vị trí nút để ở giữa màn hình. Gọi khi khởi tạo GamePanel (sau khi biết kích thước). */
    public void layoutCenter(int panelW, int panelH) {
        int bw = 260, bh = 36, gap = 14;
        int cx = (panelW - bw) / 2;
        int cy = panelH / 2;

        btnMusicToggle.setBounds(cx, cy, bw, bh);
        btnMainMenu.setBounds(cx, cy + bh + gap, bw, bh);
        btnBack.setBounds(cx, cy + 2 * (bh + gap), bw, bh);
    }

    public void render(Graphics2D g2, boolean musicOn) {
        g2.setFont(btnFont);
        drawBtn(g2, btnMusicToggle, musicOn ? "Music: ON (pause/resume)" : "Music: OFF");
        drawBtn(g2, btnMainMenu, "Return to MAIN MENU");
        drawBtn(g2, btnBack, "Back");
    }

    // ---- hit test cho GamePanel ----
    public boolean hitToggle(Point p)   { return btnMusicToggle.contains(p); }
    public boolean hitMainMenu(Point p) { return btnMainMenu.contains(p); }
    public boolean hitBack(Point p)     { return btnBack.contains(p); }

    // ---- tiện ích vẽ nút ----
    private void drawBtn(Graphics2D g2, Rectangle r, String text) {
        // nền mờ
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        // viền
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        // text
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getAscent();
        g2.drawString(text, r.x + (r.width - tw) / 2, r.y + (r.height + th) / 2 - 3);
    }
}
