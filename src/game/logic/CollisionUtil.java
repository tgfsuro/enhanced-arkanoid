package game.logic;

import game.objects.Ball;
import game.objects.Brick;
import game.objects.Paddle;

import java.awt.*;

/** Các hàm xử lý va chạm. */
public final class CollisionUtil {
    private CollisionUtil() {}

    /** Phản xạ bóng với biên panel. */
    public static void reflectWithWalls(Ball b, int W, int H) {
        if (b.x - b.r < 0)      { b.x = b.r;        b.vx = -b.vx; }
        if (b.x + b.r > W)      { b.x = W - b.r;    b.vx = -b.vx; }
        if (b.y - b.r < 0)      { b.y = b.r;        b.vy = -b.vy; }
    }

    /** Bóng–paddle: điều chỉnh hướng theo vị trí chạm. */
    public static void ballBounceOnPaddle(Ball b, Paddle paddle) {
        if (!(b.getRect().intersects(paddle.getRect()) && b.vy > 0)) return;

        b.y = paddle.y - b.r - 1;
        double center = paddle.x + paddle.w / 2.0;
        double t = (b.x - center) / (paddle.w / 2.0); // -1..1
        t = Math.max(-1, Math.min(1, t));

        double speed = 6.2;
        b.vx = t * 5.0;
        b.vy = -Math.sqrt(Math.max(1, speed*speed - b.vx*b.vx));
    }

    /** Trả về index brick bị phá hoặc -1 nếu không chạm. */
    public static int hitBrickIndex(Ball b, Brick[] bricks) {
        Rectangle br = b.getRect();
        for (int i = 0; i < bricks.length; i++) {
            Brick r = bricks[i];
            if (r == null) continue;
            Rectangle rr = r.getRect();
            if (!br.intersects(rr)) continue;

            // Xác định hướng phản xạ cơ bản
            int leftO   = (int)(br.x + br.width) - rr.x;
            int rightO  = (int)(rr.x + rr.width) - br.x;
            int topO    = (int)(br.y + br.height) - rr.y;
            int bottomO = (int)(rr.y + rr.height) - br.y;
            if (Math.min(leftO, rightO) < Math.min(topO, bottomO)) b.vx = -b.vx; else b.vy = -b.vy;

            return i;
        }
        return -1;
    }
}
