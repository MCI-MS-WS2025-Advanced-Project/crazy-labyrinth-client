package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

@Slf4j
public abstract class BaseItem<G> implements ViewItem {

    protected final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    protected final ConcurrentLinkedQueue<G> updateQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_QUEUE_SIZE = 50;

    /**
     * Queue an update and process it on the FX thread.
     *
     * @param source updated source data
     */
    public void updateFrom(G source) {
        updateQueue.offer(source);

        while (updateQueue.size() > MAX_QUEUE_SIZE) {
            updateQueue.poll();
        }

        if (pendingUpdate.compareAndSet(false, true)) {
            processNextUpdate();
        }
    }

    /**
     * Drain the update queue and apply changes on the FX thread.
     */
    private void processNextUpdate() {
        Runnable updateTask =
                () -> {
                    G source = updateQueue.poll();
                    try {
                        updateItemFromSource(source);
                    } catch (Exception e) {
                        log.error("error updating item from source", e);
                    }

                    if (!updateQueue.isEmpty()) {
                        processNextUpdate();
                    } else {
                        pendingUpdate.set(false);
                    }
                    if (!updateQueue.isEmpty() && pendingUpdate.compareAndSet(false, true)) {
                        processNextUpdate();
                    }
                };
        JavaFXUtils.runOnFxThread(updateTask);
    }

    /**
     * Update item state from the provided source.
     *
     * @param source source data
     */
    protected abstract void updateItemFromSource(G source);

    /**
     * Dispose this item on the FX thread.
     */
    @Override
    public void dispose() {
        Runnable disposeTask = this::disposeItem;
        JavaFXUtils.runOnFxThread(disposeTask);
    }

    /**
     * Dispose item-specific resources.
     */
    protected abstract void disposeItem();
}
