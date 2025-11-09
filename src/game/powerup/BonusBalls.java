package game.powerup;

import game.GamePanel;

public final class BonusBalls {
    private BonusBalls(){}

    public static void activate(GamePanel gp) {
        gp.spawnBonusBalls(2);   // thêm 2 bóng
    }
}
