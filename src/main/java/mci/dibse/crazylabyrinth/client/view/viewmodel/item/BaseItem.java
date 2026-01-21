package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public abstract class BaseItem<G> implements ViewItem {

    protected final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    protected final ConcurrentLinkedQueue<G> updateQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_QUEUE_SIZE = 50;


    public void updateFrom(G source) {
        updateQueue.offer(source);

        while (updateQueue.size() > MAX_QUEUE_SIZE) {
            updateQueue.poll();
        }


        if (pendingUpdate.compareAndSet(false, true)) {
            processNextUpdate();
        }
    }

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

    protected abstract void updateItemFromSource(G source);

    @Override
    public void dispose() {
        Runnable disposeTask = this::disposeItem;
        JavaFXUtils.runOnFxThread(disposeTask);
    }

    protected abstract void disposeItem();
}
