package mci.dibse.crazylabyrinth.client.model;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

//TODO remove
@Component
public class HelloJavaFXModel {

    @Getter
    private  StringProperty message = new SimpleStringProperty("Initial message from HelloJavaFXModel");

    public HelloJavaFXModel() {
        // Add a listener to the message property
        message.addListener((observable, oldValue, newValue) -> {
            System.out.println("State changed: Old Value = " + oldValue + ", New Value = " + newValue);
        });
    }

}



