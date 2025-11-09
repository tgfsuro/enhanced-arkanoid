package game.play;

import game.LevelManager;
import game.objects.Brick;
import game.objects.Paddle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class ProjectileManager {

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<LazerRay> lasers = new ArrayList<>();

    private long gunUntil = 0, nextBulletAt = 0;

    public void clear() { bullets.clear(); lasers.clear(); gunUntil = 0; nextBulletAt = 0; }

    /** Bật súng tự bắn (ms) */
    public void enableGun(long ms) {
        long now = System.currentTimeMillis();
        gunUntil = Math.max(gunUntil, now) + ms;
        if (nextBulletAt < now) nextBulletAt = now;
    }

    /** Bắn tia laser (xử lý trừ HP và rơi vật phẩm) */
    public void fireLaser(int xCenter, LevelManager levels, DropManager dropMgr, IntConsumer addScore) {
        int halfW = 4;
        for (int i = 0; i < levels.bricks.length; i++) {
            Brick bk = levels.bricks[i];
            if (bk == null || bk.isUnbreakable()) continue;
            boolean sameCol = (xCenter + halfW >= bk.x) && (xCenter - halfW <= bk.x + bk.w);
            if (!sameCol) continue;

            boolean broken = bk.onHit();
            if (broken) {
                if (bk.getPickup() != null) {
                    dropMgr.spawn(bk.getPickup(), bk.x + bk.w/2.0, bk.y + bk.h/2.0);
                }
                levels.bricks[i] = null;
                addScore.accept(10);
            } else {
                addScore.accept(2);
            }
        }
        lasers.add(new LazerRay(xCenter, System.currentTimeMillis() + 120));
    }

    /** Update bullets + lasers; sinh đạn khi đang có súng */
    public void update(long now, Paddle paddle, int height, LevelManager levels,
                       DropManager dropMgr, IntConsumer addScore)
    {
        // auto fire
        if (now < gunUntil && now >= nextBulletAt) {
            bullets.add(new Bullet((int)(paddle.x + paddle.w/2.0), (int)paddle.y, -12));
            nextBulletAt = now + 200;
        }

        // bullets
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            b.step();
            if (b.outOfTop()) { bullets.remove(i--); continue; }
            Rectangle r = b.rect();
            for (int j = 0; j < levels.bricks.length; j++) {
                Brick bk = levels.bricks[j];
                if (bk == null || bk.isUnbreakable()) continue;
                if (!r.intersects(bk.getRect())) continue;

                boolean broken = bk.onHit();
                if (broken) {
                    if (bk.getPickup() != null) {
                        dropMgr.spawn(bk.getPickup(), bk.x + bk.w/2.0, bk.y + bk.h/2.0);
                    }
                    levels.bricks[j] = null;
                    addScore.accept(10);
                } else {
                    addScore.accept(2);
                }
                bullets.remove(i--);
                break;
            }
        }

        // lasers lifetime
        for (int i = 0; i < lasers.size(); i++) {
            if (lasers.get(i).expired(now)) lasers.remove(i--);
        }
    }

    /** Vẽ đạn & tia */
    public void render(Graphics2D g2, Paddle paddle) {
        g2.setColor(Color.YELLOW);
        for (Bullet b : bullets) g2.fillRect(b.x - 2, b.y - 8, 4, 8);

        g2.setColor(new Color(0, 255, 255, 180));
        for (LazerRay l : lasers) {
            g2.fillRect(l.x - 2, 0, 4, (int)paddle.y);
            g2.fillOval(l.x - 4, (int)paddle.y - 6, 8, 8);
        }
    }
}
