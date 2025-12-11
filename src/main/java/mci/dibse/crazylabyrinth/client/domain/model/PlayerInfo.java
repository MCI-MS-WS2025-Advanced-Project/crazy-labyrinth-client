package mci.dibse.crazylabyrinth.client.domain.model;

import labyrinth.contracts.models.PlayerColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mci.dibse.crazylabyrinth.client.domain.model.enums.GameStatus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.util.Map;


//TODO remove?
@Setter @Getter
public class PlayerInfo {


    private String id;
    private String name;
    private PlayerColor color;
    private Boolean isAdmin;
    private Boolean isAiControlled;
    private Map<String, Object> additionalProperties;

    //TODO notwendig?
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    public void removeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        propertyChangeSupport.firePropertyChange("id", oldValue, id);
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        propertyChangeSupport.firePropertyChange("name", oldValue, name);
    }

    public void setColor(PlayerColor color) {
        PlayerColor oldValue = this.color;
        this.color = color;
        propertyChangeSupport.firePropertyChange("color", oldValue, color);
    }

    public void setIsAdmin(Boolean isAdmin) {
        Boolean oldValue = this.isAdmin;
        this.isAdmin = isAdmin;
        propertyChangeSupport.firePropertyChange("isAdmin", oldValue, isAdmin);
    }

    public void setIsAiControlled(Boolean isAiControlled) {
        Boolean oldValue = this.isAiControlled;
        this.isAiControlled = isAiControlled;
        propertyChangeSupport.firePropertyChange("isAiControlled", oldValue, isAiControlled);
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        Map<String, Object> oldValue = this.additionalProperties;
        this.additionalProperties = additionalProperties;
        propertyChangeSupport.firePropertyChange("additionalProperties", oldValue, additionalProperties);
    }

}
