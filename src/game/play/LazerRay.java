package game.play;

public class LazerRay {
    public final int x;
    public final long endTime;
    public LazerRay(int x, long endTime) { this.x = x; this.endTime = endTime; }
    public boolean expired(long now) { return now >= endTime; }
}
