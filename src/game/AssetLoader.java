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
 * - Ảnh/Text: ưu tiên classpath, fallback đọc từ filesystem (src/, src/resources, resources, ...).
 * - Nhạc: ưu tiên Java Sound Clip (WAV/AIFF/AU, và MP3 nếu đã thêm mp3spi + tritonus_share).
 *   Nếu không mở được MP3 bằng Clip, fallback sang JLayer (jl1.0.1.jar).
 */
public final class AssetLoader {
    private static final Map<String, BufferedImage> IMG_CACHE = new HashMap<>();
    private AssetLoader() {}

    // ========================== path utils ==========================
    private static String norm(String p) {
        if (p == null) return "";
        String s = p.replace('\\', '/').trim();
        while (s.startsWith("/")) s = s.substring(1);
        return s;
    }
    private static boolean isAbsolutePath(String p) {
        if (p == null || p.isEmpty()) return false;
        // Windows absolute "C:/..." hoặc có ":" trong ký tự thứ 2
        if (p.length() > 1 && p.charAt(1) == ':') return true;
        // Unix-like
        return p.startsWith("/") || p.startsWith("\\");
    }

    // ========================== generic open ==========================
    private static InputStream open(String resPath) throws IOException {
        String rp = norm(resPath);

        // 1) classpath trước
        InputStream in = AssetLoader.class.getClassLoader().getResourceAsStream(rp);
        if (in != null) return in;

        // 2) nếu người dùng truyền đường dẫn tuyệt đối / tương đối hiện hành → thử trực tiếp
        File direct = new File(rp);
        if (direct.exists() && direct.isFile()) {
            return new FileInputStream(direct);
        }

        // 3) thử các gốc filesystem phổ biến
        //    Ưu tiên "" và "." để chạy được khi working dir = root project
        String[] roots = {
                "", ".",
                "src/",
                "src/resources/",
                "resources/",
                "src/main/resources/"
        };
        for (String root : roots) {
            File f = new File(root + rp);
            if (f.exists() && f.isFile()) {
                return new FileInputStream(f);
            }
        }

        // 4) Nếu vẫn không có, in cảnh báo đường dẫn đã thử (giúp debug)
        StringBuilder tried = new StringBuilder();
        tried.append("classpath:/").append(rp).append('\n');
        tried.append(rp).append('\n');
        for (String root : roots) tried.append(root).append(rp).append('\n');

        throw new FileNotFoundException(
                "Resource not found: " + resPath + "\nTried:\n" + tried
        );
    }

    private static boolean existsFile(String absPath) {
        return absPath != null && new File(absPath).exists();
    }

    // ============================ images =============================
    /** Ném lỗi nếu không load được (dùng cho sản phẩm). */
    public static BufferedImage image(String resPath) {
        String key = norm(resPath);
        BufferedImage cached = IMG_CACHE.get(key);
        if (cached != null) return cached;
        try (InputStream in = open(key)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("ImageIO.read returned null for: " + key);
            IMG_CACHE.put(key, img);
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException("load image fail: " + key, e);
        }
    }

    /** Trả về null nếu không load được (tiện debug/ thử skin). */
    public static BufferedImage imageOrNull(String resPath) {
        String key = norm(resPath);
        BufferedImage cached = IMG_CACHE.get(key);
        if (cached != null) return cached;
        try (InputStream in = open(key)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            IMG_CACHE.put(key, img);
            return img;
        } catch (IOException e) {
            System.err.println("[AssetLoader] imageOrNull failed: " + key + " (" + e.getMessage() + ")");
            return null;
        }
    }

    public static Image scaled(String resPath, int w, int h) {
        BufferedImage img = image(resPath);
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    // ============================= text ==============================
    public static java.util.List<String> readLines(String resPath) {
        String key = norm(resPath);
        try (InputStream in = open(key);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            java.util.List<String> lines = new ArrayList<>();
            for (String s; (s = br.readLine()) != null; ) {
                s = s.strip();
                if (!s.isEmpty()) lines.add(s);
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("read text fail: " + key, e);
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
        String key = norm(path);
        try {
            Clip c = openClip(open(key));
            return new ClipMusic(c, true);
        } catch (Throwable clipFail) {
            try {
                Class<?> clazz = Class.forName("javazoom.jl.player.Player");
                Thread th = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try (InputStream in = open(key)) {
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
                System.err.println("[AUDIO] Không phát được: " + key + " (" + t.getMessage() + ")");
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
