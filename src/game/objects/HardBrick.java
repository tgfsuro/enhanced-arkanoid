package game.objects;

import game.play.Pickup;
import java.awt.*;

/** Gạch cứng: cần 2 hit để vỡ (hp = 2), màu xanh lá. Có thể chứa powerup. */
public class HardBrick extends Brick {

    public HardBrick(int x, int y, int w, int h) {
        this(x, y, w, h, null);
    }

    public HardBrick(int x, int y, int w, int h, Pickup.Type pickup) {
        super(
                x, y, w, h,
                false,                      // không bất tử
                new Color(40, 180, 90),     // xanh lá
                pickup,                     // có thể null
                2                           // 2 hit
        );
    }
}
