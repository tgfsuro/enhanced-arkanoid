package game.powerup;

import game.GamePanel;

public final class BonusBalls {
    private BonusBalls(){}

    public static void activate(GamePanel gp) {
        // mỗi bóng hiện có sẽ sinh thêm 2 bóng (tổng thành x3)
        gp.spawnBonusBalls();
    }
}
