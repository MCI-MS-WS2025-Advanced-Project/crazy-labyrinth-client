package mci.dibse.crazylabyrinth.client.domain.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import org.springframework.stereotype.Component;

// TODO remove
@Component
public class HelloJavaFXModel {

    @Getter
    private StringProperty message =
            new SimpleStringProperty("Initial message from HelloJavaFXModel");

    public HelloJavaFXModel() {
        // Add a listener to the message property
        message.addListener(
                (observable, oldValue, newValue) -> {
                    System.out.println(
                            "State changed: Old Value = " + oldValue + ", New Value = " + newValue);
                });
    }
}
