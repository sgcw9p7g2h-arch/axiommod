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

    /** Moves the current task to the end so later blocks can provide support. */
    public void defer() {
        BuildTask task = pending.poll();
        if (task != null) pending.add(task);
    }

    public void complete() { completed++; }
    public void skip() { skipped++; }
    public int remaining() { return pending.size(); }
    public int completed() { return completed; }
    public int skipped() { return skipped; }
}
