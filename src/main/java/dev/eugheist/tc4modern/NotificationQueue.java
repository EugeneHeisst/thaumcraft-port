package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;

/** Millisecond scheduling from TC4 PlayerNotifications; the caller supplies monotonic time. */
public final class NotificationQueue {
    public static final int DELAY = 5000;
    public record Entry(String text, long expire, long created, net.minecraft.resources.ResourceLocation image, int color) {
        public Entry(String text, long expire, long created) { this(text, expire, created, null, 0xFFFFFF); }
    }
    private List<Entry> entries = new ArrayList<>();
    private final java.util.function.IntSupplier duration;
    public NotificationQueue(){this(()->DELAY);}
    public NotificationQueue(java.util.function.IntSupplier duration){this.duration=duration;}
    public int delay(){return Math.max(4,duration.getAsInt());}

    public void add(String text, long time) { add(text, time, null, 0xFFFFFF); }
    /** addNotification(text, image, color): aspect gains carry the aspect icon tinted with its colour. */
    public void add(String text, long time, net.minecraft.resources.ResourceLocation image, int color) {
        int delay=delay();
        long bonus = entries.isEmpty() ? delay / 2 : 0;
        entries.add(new Entry(text, time + delay + bonus, time + delay / 4, image, color));
    }

    public List<Entry> update(long time) {
        int delay=delay();
        var remaining = new ArrayList<Entry>();
        boolean first = true;
        for (var entry : entries) {
            if (entry.expire() >= time) {
                remaining.add(first ? entry : new Entry(entry.text(), time + delay, entry.created(), entry.image(), entry.color()));
            }
            // Original advances this flag even when the first entry expired this frame.
            first = false;
        }
        entries = remaining;
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
