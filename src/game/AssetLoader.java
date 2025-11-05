package game;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Load ảnh/âm thanh/text từ classpath (resources). */
public final class AssetLoader {
    private static final Map<String, BufferedImage> IMG_CACHE = new HashMap<>();
    private static final Map<String, Clip> SND_CACHE = new HashMap<>();

    private AssetLoader() {}

    // ---------- Images ----------
    public static BufferedImage image(String resPath) {
        BufferedImage cached = IMG_CACHE.get(resPath);
        if (cached != null) return cached;
        try (InputStream in = AssetLoader.class.getClassLoader().getResourceAsStream(resPath)) {
            if (in == null) throw new IllegalStateException("Resource not found: " + resPath);
            BufferedImage img = ImageIO.read(in);
            IMG_CACHE.put(resPath, img);
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException("load image fail: " + resPath, e);
        }
    }

    public static Image scaled(String resPath, int w, int h) {
        return image(resPath).getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    // ---------- Sounds (WAV) ----------
    public static Clip sound(String resPath) {
        Clip cached = SND_CACHE.get(resPath);
        if (cached != null) return cached;
        try (InputStream raw = AssetLoader.class.getClassLoader().getResourceAsStream(resPath)) {
            if (raw == null) throw new IllegalStateException("Resource not found: " + resPath);
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                SND_CACHE.put(resPath, clip);
                return clip;
            }
        } catch (Exception e) {
            throw new RuntimeException("load sound fail: " + resPath, e);
        }
    }

    public static void playOnce(String resPath) {
        try {
            Clip c = sound(resPath);
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.start();
        } catch (RuntimeException ignore) {}
    }

    public static Clip loop(String resPath) {
        try {
            Clip c = sound(resPath);
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            return c;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------- Text (levels) ----------
    public static java.util.List<String> readLines(String resPath) {
        try (InputStream in = AssetLoader.class.getClassLoader().getResourceAsStream(resPath)) {
            if (in == null) throw new IllegalStateException("Resource not found: " + resPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            java.util.List<String> lines = new ArrayList<>();
            for (String s; (s = br.readLine()) != null; ) {
                s = s.strip();
                if (!s.isEmpty()) lines.add(s);
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("read text fail: " + resPath, e);
        }
    }
}
