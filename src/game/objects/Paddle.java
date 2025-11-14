package game.objects;

import game.AssetLoader;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Paddle {
    public double x, y; // top-left
    public int w, h;
    public double speed;

    // skin
    private BufferedImage skin;       // có thể null -> fallback vẽ rect
    private String skinPath;          // để debug/đổi skin lúc chạy nếu cần

    public Paddle(double x, double y, int w, int h, double speed) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.speed = speed;
    }

    /** Gán skin từ đường dẫn resource/file. Nếu lỗi -> giữ null để vẽ rect. */
    public void setSkinPath(String path) {
        this.skinPath = path;
        try { this.skin = AssetLoader.image(path); }
        catch (Throwable ignore) { this.skin = null; }
    }
    public String getSkinPath() { return skinPath; }

    public void move(int dir, int screenW) {
        x += dir * speed;
        if (x < 10) x = 10;
        if (x + w > screenW - 10) x = screenW - 10 - w;
    }

    public Rectangle getRect() { return new Rectangle((int)x, (int)y, w, h); }

    public void draw(Graphics2D g2) {
        if (skin != null) {
            // vẽ ảnh đã scale theo w×h
            g2.drawImage(skin, (int)x, (int)y, w, h, null);
            return;
        }
        // fallback: thanh trắng bo tròn
        g2.setColor(new Color(230,230,230));
        g2.fillRoundRect((int)x, (int)y, w, h, 10, 10);
    }
}
