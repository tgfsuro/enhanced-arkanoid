package game;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AssetLoader: load ảnh, text và nhạc.
 * - Ảnh/Text: ưu tiên classpath, fallback đọc từ filesystem (src/resources, resources, ...).
 * - Nhạc: ưu tiên Java Sound Clip (WAV/AIFF/AU, và MP3 nếu đã thêm mp3spi + tritonus_share).
 *   Nếu không mở được MP3 bằng Clip, fallback sang JLayer (jl1.0.1.jar).
 *   Với Clip → hỗ trợ pause/resume thật; JLayer → pause sẽ dừng, resume sẽ phát lại từ đầu.
 */
public final class AssetLoader {
    private static final Map<String, BufferedImage> IMG_CACHE = new HashMap<>();
    private AssetLoader() {}

    // ========================== generic open ==========================
    private static InputStream open(String resPath) throws IOException {
        // 1) classpath
        InputStream in = AssetLoader.class.getClassLoader().getResourceAsStream(resPath);
        if (in != null) return in;
        // 2) thử trên filesystem
        String[] roots = { "src/resources/", "resources/", "src/main/resources/", "" };
        for (String root : roots) {
            File f = new File(root + resPath);
            if (f.exists() && f.isFile()) return new FileInputStream(f);
        }
        throw new FileNotFoundException("Resource not found anywhere: " + resPath);
    }
    private static boolean existsFile(String absPath) { return absPath != null && new File(absPath).exists(); }

    // ============================ images =============================
    public static BufferedImage image(String resPath) {
        BufferedImage cached = IMG_CACHE.get(resPath);
        if (cached != null) return cached;
        try (InputStream in = open(resPath)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("ImageIO.read returned null for: " + resPath);
            IMG_CACHE.put(resPath, img);
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException("load image fail: " + resPath, e);
        }
    }
    public static Image scaled(String resPath, int w, int h) { return image(resPath).getScaledInstance(w, h, Image.SCALE_SMOOTH); }

    // ============================= text ==============================
    public static java.util.List<String> readLines(String resPath) {
        try (InputStream in = open(resPath);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
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

    // ============================== music ============================
    /** Giao diện nhạc có pause/resume/stop. */
    public interface Music {
        void pause();
        void resume();
        void stop();
        boolean isRunning();
    }

    /** Triển khai bằng Java Sound Clip (hỗ trợ resume thật). */
    private static final class ClipMusic implements Music {
        private final Clip clip;
        private long pauseMicros = 0;
        private boolean running = false;

        ClipMusic(Clip clip, boolean startLooping) {
            this.clip = clip;
            if (startLooping) {
                this.clip.loop(Clip.LOOP_CONTINUOUSLY);
                this.clip.start();
                running = true;
            }
        }
        @Override public void pause() {
            if (!running) return;
            pauseMicros = clip.getMicrosecondPosition();
            clip.stop();
            running = false;
        }
        @Override public void resume() {
            if (running) return;
            clip.setMicrosecondPosition(Math.max(0, pauseMicros));
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            running = true;
        }
        @Override public void stop() {
            try { clip.stop(); } catch (Exception ignored) {}
            try { clip.close(); } catch (Exception ignored) {}
            running = false;
        }
        @Override public boolean isRunning() { return running && clip.isOpen(); }
    }

    /** Triển khai fallback JLayer (không resume được – resume = phát lại từ đầu). */
    private static final class JLayerMusic implements Music {
        private final Thread t;
        private volatile boolean running = true;
        JLayerMusic(Thread t) { this.t = t; }
        @Override public void pause() { stop(); }
        @Override public void resume() {
            System.err.println("[MP3/JLayer] Resume không hỗ trợ – sẽ phát lại từ đầu (khuyên dùng mp3spi để resume).");
        }
        @Override public void stop() {
            running = false;
            if (t != null) { t.interrupt(); try { t.join(300); } catch (InterruptedException ignored) {} }
        }
        @Override public boolean isRunning() { return running && t != null && t.isAlive(); }
    }

    /** Tạo Clip từ stream; tự convert sang PCM nếu cần (cần mp3spi để mở MP3). */
    private static Clip openClip(InputStream in) throws Exception {
        try (BufferedInputStream bin = new BufferedInputStream(in)) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(bin);
            AudioFormat base = ais.getFormat();
            AudioFormat target = base;
            if (base.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                target = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        base.getSampleRate(),
                        16,
                        base.getChannels(),
                        base.getChannels() * 2,
                        base.getSampleRate(),
                        false
                );
                ais = AudioSystem.getAudioInputStream(target, ais); // cần mp3spi đối với MP3
            }
            DataLine.Info info = new DataLine.Info(Clip.class, target);
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
            return clip;
        }
    }

    /** Tạo Music từ resource path: ưu tiên Clip (resume được), nếu lỗi → JLayer loop. */
    public static Music loopMusicFromResource(String path) {
        // thử Clip trước
        try {
            Clip c = openClip(open(path));
            return new ClipMusic(c, true);
        } catch (Throwable clipFail) {
            // fallback JLayer
            try {
                Class<?> clazz = Class.forName("javazoom.jl.player.Player");
                Thread th = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try (InputStream in = open(path)) {
                            Object player = clazz.getConstructor(InputStream.class).newInstance(in);
                            clazz.getMethod("play").invoke(player); // chạy hết bài
                        } catch (Throwable e) { break; }
                    }
                }, "mp3-jlayer-loop");
                th.setDaemon(true);
                th.start();
                System.err.println("[MP3] Đang dùng JLayer fallback – pause sẽ không resume.");
                return new JLayerMusic(th);
            } catch (Throwable t) {
                System.err.println("[AUDIO] Không phát được: " + path + " (" + t.getMessage() + ")");
                return null;
            }
        }
    }

    /** Tạo Music từ đường file tuyệt đối: ưu tiên Clip, lỗi → JLayer loop. */
    public static Music loopMusicFromFile(String absPath) {
        if (!existsFile(absPath)) return null;
        try (InputStream in = new FileInputStream(absPath)) {
            Clip c = openClip(in);
            return new ClipMusic(c, true);
        } catch (Throwable clipFail) {
            try {
                Class<?> clazz = Class.forName("javazoom.jl.player.Player");
                Thread th = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try (InputStream in = new FileInputStream(absPath)) {
                            Object player = clazz.getConstructor(InputStream.class).newInstance(in);
                            clazz.getMethod("play").invoke(player);
                        } catch (Throwable e) { break; }
                    }
                }, "mp3-jlayer-loop");
                th.setDaemon(true);
                th.start();
                System.err.println("[MP3] Đang dùng JLayer fallback – pause sẽ không resume.");
                return new JLayerMusic(th);
            } catch (Throwable t) {
                System.err.println("[AUDIO] Không phát được file: " + absPath + " (" + t.getMessage() + ")");
                return null;
            }
        }
    }
}
