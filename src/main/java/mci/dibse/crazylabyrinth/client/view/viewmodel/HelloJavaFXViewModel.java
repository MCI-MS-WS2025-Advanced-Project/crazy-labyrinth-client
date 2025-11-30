package mci.dibse.crazylabyrinth.client.view.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mci.dibse.crazylabyrinth.client.model.HelloJavaFXModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//TODO Remove
@Component
public class HelloJavaFXViewModel {

    private final HelloJavaFXModel model;

    @Getter
    private StringProperty message;


    public HelloJavaFXViewModel(HelloJavaFXModel model) {
        this.model = model;
        this.message = new SimpleStringProperty();
        this.message.bindBidirectional(model.getMessage());
    }



    public void onButtonClicked() {
        System.out.println("button clicked");
        message.set("Hello from JavaFXViewModel!");
    }


}
