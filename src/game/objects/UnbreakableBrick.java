package game.objects;

import java.awt.*;

/** Gạch bất tử (không thể phá), màu xám đậm. */
public class UnbreakableBrick extends Brick {
    public UnbreakableBrick(int x, int y, int w, int h) {
        super(
                x, y, w, h,
                true,                       // bất tử
                new Color(130,130,140),     // xám
                null,                       // không có powerup
                Integer.MAX_VALUE           // hp vô hạn
        );
    }
}
