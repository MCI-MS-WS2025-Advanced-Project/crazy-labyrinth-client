package mci.dibse.crazylabyrinth.client.view.controller.impl;

import javafx.scene.Node;
import mci.dibse.crazylabyrinth.client.view.controller.ViewController;

public class ViewControllerImpl implements ViewController {
    protected Node root;

    public void setRoot(Node root) {
        this.root = root;
    }
}
