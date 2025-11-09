package game.powerup;

import game.GamePanel;

public final class Gun {
    private Gun() {}
    public static void activate(GamePanel gp) {
        gp.enableGun(5_000); // bắn 5s
    }
}
