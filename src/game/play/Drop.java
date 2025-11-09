package game.play;

import java.awt.*;
import java.awt.Rectangle;

public class Drop {
    public final Pickup.Type type;
    public double x, y;
    public int size = 18;
    public double vy = 2.0;

    public Drop(Pickup.Type t, double x, double y) {
        this.type = t; this.x = x; this.y = y;
    }

    public void step() { y += vy; }
    public boolean outOfBottom(int h) { return y > h; }
    public Rectangle rect() { return new Rectangle((int)(x - size/2), (int)(y - size/2), size, size); }

    public void draw(Graphics2D g2) {
        Color c = switch (type) {
            case EXPAND       -> new Color(50, 200, 255);
            case BONUS_BALLS  -> new Color(255, 200, 50);
            case LAZER        -> new Color(120, 255, 120);
            case GUN          -> new Color(255, 100, 140);
        };
        g2.setColor(c);
        g2.fillOval((int)(x - size/2), (int)(y - size/2), size, size);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        String s = switch (type) {
            case EXPAND -> "E"; case BONUS_BALLS -> "B"; case LAZER -> "L"; case GUN -> "G";
        };
        g2.drawString(s, (int)x - 4, (int)y + 4);
    }
}
