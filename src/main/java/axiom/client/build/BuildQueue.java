package axiom.client.build;

import java.util.ArrayDeque;

public final class BuildQueue {
    private final ArrayDeque<BuildTask> pending = new ArrayDeque<>();
    private int completed;
    private int skipped;

    public void clear() {
        pending.clear();
        completed = 0;
        skipped = 0;
    }

    public void add(BuildTask task) { pending.add(task); }
    public BuildTask peek() { return pending.peek(); }
    public BuildTask poll() { return pending.poll(); }
    public void complete() { completed++; }
    public void skip() { skipped++; }
    public int remaining() { return pending.size(); }
    public int completed() { return completed; }
    public int skipped() { return skipped; }
}
