package dev.eugheist.tc4modern;

public final class NotificationQueueTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void run() {
        var queue = new NotificationQueue();
        queue.add("first", 1000);
        require(queue.update(1000).getFirst().expire() == 8500, "First notification gets initial grace period");
        require(queue.update(1000).getFirst().created() == 2250, "Appearance animation lasts 1250ms");
        queue.add("second", 1100);
        queue.update(5000); // Render frames keep pending entries alive before their initial expiry.
        var waiting = queue.update(8000);
        require(waiting.size() == 2 && waiting.get(1).expire() == 13000, "Queued notification must not expire while waiting");
        require(queue.update(8500).size() == 2, "Expiry boundary is inclusive");
        var next = queue.update(8501);
        require(next.size() == 1 && next.getFirst().text().equals("second"), "Expired head must expose next notification");
        require(next.getFirst().expire() == 13501, "Next gets a full interval at head transition");
        require(queue.update(13502).isEmpty(), "Final notification must expire");
        queue.add("new", 14000);
        require(queue.update(14000).getFirst().expire() == 21500, "Empty queue starts with grace period again");
        queue.clear();
        require(queue.update(14000).isEmpty(), "Disconnect cleanup must clear queued notices");
        System.out.println("Notification queue tests passed: timing, waiting, expiry, cleanup.");
    }
}
