package mci.dibse.crazylabyrinth.client.view.viewmodel.item;


/**
 *       Interface for cleanup of unused references to avoid memory leaks
 *       Remove listener to prevent memory leaks
 *       Clear collections and remove listeners
 *       Clear properties
 *       Clear references
 */
public interface Disposable {
    void dispose();
}
