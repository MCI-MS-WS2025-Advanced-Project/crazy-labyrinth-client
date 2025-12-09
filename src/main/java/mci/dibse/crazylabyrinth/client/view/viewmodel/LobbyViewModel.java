package mci.dibse.crazylabyrinth.client.view.viewmodel;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.NavigationController;
import mci.dibse.crazylabyrinth.client.domain.model.LobbyState;
import mci.dibse.crazylabyrinth.client.service.LobbyStateUpdateListener;
import mci.dibse.crazylabyrinth.client.service.ServerEventService;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.LobbyStateItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.navigation.NavigationStateViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LobbyViewModel implements LobbyStateUpdateListener {

    @Autowired
    private NavigationStateViewModel navState;
    @Autowired
    private ServerEventService serverEventService;

    private LobbyStateItem lobbyStateItem;


    @PostConstruct
    public void init(){
        serverEventService.addListener(this);
    }

    public void setUpLobby(LobbyStateItem lobbyStateItem){
        this.lobbyStateItem = lobbyStateItem;
        navState.navigateTo(NavigationStateViewModel.ScreenId.LOBBY);
    }

    @Override
    public void onLobbyStateUpdated(LobbyState lobbyState) {
        LobbyStateItem item = new LobbyStateItem(lobbyState);
        setUpLobby(item);
    }
}
