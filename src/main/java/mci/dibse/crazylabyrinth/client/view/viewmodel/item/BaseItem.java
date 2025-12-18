package mci.dibse.crazylabyrinth.client.view.viewmodel.item;

import mci.dibse.crazylabyrinth.client.infrastructure.util.JavaFXUtils;

public abstract class BaseItem<G> implements ViewItem {

    protected final java.util.concurrent.atomic.AtomicBoolean pendingUpdate =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    public void updateFrom(G source) {
        if (!pendingUpdate.compareAndSet(false, true)) return;

        Runnable updateTask =
                () -> {
                    try {
                        updateItemFromSource(source);
                    } finally {
                        pendingUpdate.set(false);
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
