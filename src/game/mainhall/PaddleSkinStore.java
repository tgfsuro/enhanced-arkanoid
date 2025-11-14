package game.mainhall;

import game.AssetLoader;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Lưu lựa chọn skin paddle + cung cấp metadata cho màn chọn paddle. */
public final class PaddleSkinStore {
    // Danh sách đường dẫn & tên hiển thị (bạn có thể đổi/ thêm bớt)
    private static final String[] SKIN_PATHS = {
            "game/mainhall/paddle/paddle1.png",
            "game/mainhall/paddle/paddle2.png",
            "game/mainhall/paddle/paddle3.png",
            "game/mainhall/paddle/paddle4.png",
            "game/mainhall/paddle/paddle5.png"
    };
    private static final String[] SKIN_NAMES = {
            "Paddle 1",
            "Paddle 2",
            "Paddle 3",
            "Paddle 4",
            "Paddle 5"
    };

    // chỉ số đang chọn (0..count-1)
    private static int index = 0;

    private PaddleSkinStore() {}

    // ===== API dùng trong GamePanel =====
    public static void setIndex(int i) {
        if (i < 0) i = 0;
        if (i >= count()) i = count() - 1;
        index = i;
    }
    public static int getIndex() { return index; }

    /** Ảnh skin hiện tại (có thể null nếu không tìm thấy file). */
    public static Image getImage() { return getImageAt(index); }

    // ===== API cho PaddleSelectWindow =====
    public static int count() { return SKIN_PATHS.length; }

    /** Tên hiển thị của skin i (an toàn index). */
    public static String nameAt(int i) {
        if (i < 0 || i >= count()) return "Paddle";
        return (i < SKIN_NAMES.length && SKIN_NAMES[i] != null) ? SKIN_NAMES[i] : ("Paddle " + (i+1));
        // Nếu muốn lấy theo tên file:
        // String path = SKIN_PATHS[i];
        // return path.substring(path.lastIndexOf('/')+1);
    }

    /** Ảnh skin tại index i (an toàn index). */
    public static Image getImageAt(int i) {
        if (i < 0 || i >= count()) return null;
        String path = SKIN_PATHS[i];
        BufferedImage img = AssetLoader.imageOrNull(path);
        if (img == null) {
            System.err.println("[PaddleSkin] NOT FOUND: " + path);
            return null;
        }
        System.out.println("[PaddleSkin] LOADED: " + path + " size=" + img.getWidth() + "x" + img.getHeight());
        return img;
    }
}
