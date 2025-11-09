package game.play;

import game.objects.Paddle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropManager {
    private final List<Drop> drops = new ArrayList<>();

    public List<Drop> list() { return drops; }
    public void clear() { drops.clear(); }

    public void spawn(Pickup.Type t, double x, double y) {
        if (t != null) drops.add(new Drop(t, x, y));
    }

    /** Cập nhật rơi + ăn vật phẩm */
    public void update(Paddle paddle, int height, Consumer<Pickup.Type> apply) {
        for (int i = 0; i < drops.size(); i++) {
            Drop d = drops.get(i);
            d.step();
            if (d.outOfBottom(height)) { drops.remove(i--); continue; }
            if (d.rect().intersects(paddle.getRect())) {
                apply.accept(d.type);
                drops.remove(i--);
            }
        }
    }

    public void render(Graphics2D g2) {
        for (Drop d : drops) d.draw(g2);
    }
}
