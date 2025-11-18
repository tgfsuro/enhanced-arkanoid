package game.objects;

import game.play.Pickup; // chứa enum Pickup.Type
import java.awt.*;

/** Gạch cơ bản. Hỗ trợ: HP (gạch cứng), bất tử, powerup-embedded, vẽ logo powerup. */
public class Brick {

    public final int x, y, w, h;
    private final boolean unbreakable;
    private final Color baseColor;
    private final Pickup.Type pickup;   // null nếu không có quà
    private int hp;                     // 1 = vỡ 1 hit; >1 là gạch cứng; bất tử dùng Integer.MAX_VALUE

    public Brick(int x, int y, int w, int h,
                 boolean unbreakable, Color color,
                 Pickup.Type pickup, int hp) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.unbreakable = unbreakable;
        this.baseColor = (color != null ? color : new Color(120, 170, 255));
        this.pickup = pickup;                           // có thể null
        this.hp = Math.max(1, hp);
        if (unbreakable) this.hp = Integer.MAX_VALUE;   // không thể phá
    }

    /** Bị đánh trúng 1 lần. Trả về true nếu gạch vỡ. */
    public boolean onHit() {
        if (unbreakable) return false;
        if (hp > 1) { hp -= 1; return false; }
        hp = 0; return true;
    }

    public boolean isUnbreakable() { return unbreakable; }
    public boolean isDestroyed()   { return hp <= 0; }

    /** Powerup gắn trong gạch (có thể null). */
    public Pickup.Type getPowerup() { return pickup; }
    /** Alias để tương thích với code cũ. */
    public Pickup.Type getPickup()  { return pickup; }
    public Pickup.Type hasPickup()  { return pickup; }

    public Rectangle getRect() { return new Rectangle(x, y, w, h); }

    /** Màu hiển thị tùy trạng thái + loại power-up. */
    private Color colorForRender() {
        if (unbreakable) return new Color(130, 130, 140); // xám “bất tử”
        // Nếu có power-up → ưu tiên màu theo yêu cầu
        if (pickup != null) {
            return switch (pickup) {
                case HEART        -> new Color(220, 60, 60);    // đỏ
                case GUN          -> new Color(255, 210, 0);    // vàng
                case LAZER        -> new Color(80, 220, 120);   // xanh lá
                case EXPAND       -> new Color(160, 90, 220);   // tím
                case BONUS_BALLS  -> new Color(255, 160, 60);   // cam
            };
        }
        // Không có power-up: cứng thì xanh lá đậm, thường dùng baseColor
        if (hp >= 2)     return new Color(40, 180, 90);
        return baseColor;
    }

    /** Vẽ gạch + viền + logo powerup (nếu có). */
    public void draw(Graphics2D g2) {
        Color c = colorForRender();
        g2.setColor(c);
        g2.fillRoundRect(x, y, w, h, 6, 6);

        // viền
        g2.setColor(c.darker());
        g2.drawRoundRect(x, y, w, h, 6, 6);

        // logo powerup gắn trên mặt gạch
        if (pickup != null) {
            String letter = switch (pickup) {
                case EXPAND -> "E";
                case BONUS_BALLS -> "B";
                case LAZER -> "L";
                case GUN -> "G";
                case HEART -> "H";
            };
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            // Chọn màu chữ cho nổi trên nền
            g2.setColor(Color.BLACK);
            int tw = g2.getFontMetrics().stringWidth(letter);
            int th = g2.getFontMetrics().getAscent();
            g2.drawString(letter, x + (w - tw) / 2, y + (h + th) / 2 - 3);
        }

        // gạch cứng: chấm trắng hiển thị HP (tối đa 3 chấm cho đẹp)
        if (!unbreakable && hp >= 2) {
            g2.setColor(new Color(255,255,255,200));
            int dots = Math.min(3, hp);
            int gap = 6, sz = 4;
            int total = dots * sz + (dots - 1) * gap;
            int sx = x + (w - total) / 2;
            int sy = y + h - 8;
            for (int i = 0; i < dots; i++) {
                g2.fillOval(sx + i * (sz + gap), sy, sz, sz);
            }
        }
    }
}
