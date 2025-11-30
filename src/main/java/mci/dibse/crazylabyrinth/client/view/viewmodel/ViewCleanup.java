package mci.dibse.crazylabyrinth.client.view.viewmodel;


import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;

/**         Interface for cleanup of unused elements before e.g. view change
 *          1. Clear property bindings
 *          2. Clear event handlers
 *          3. Clear table cell factories (breaks reference chains)
 *          4. Clear table data
 *          5. Clear combo box items
 *
 */
public interface ViewCleanup {

    void cleanup();
}
