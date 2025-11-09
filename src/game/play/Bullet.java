package game.play;

import java.awt.Rectangle;

public class Bullet {
    public int x, y, vy;
    public Bullet(int x, int y, int vy) { this.x = x; this.y = y; this.vy = vy; }
    public void step() { y += vy; }
    public boolean outOfTop() { return y < -20; }
    public Rectangle rect() { return new Rectangle(x - 2, y - 8, 4, 8); }
}
