package com.orbitmessenger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.orbitmessenger.Controllers.*;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("./FXML/mainView.fxml"));
        MainController mc = new MainController();
        loader.setController(mc);
        Parent root = (Parent) loader.load();
        //primaryStage.getIcons().add(new Image("./images/orbit.png"));
        primaryStage.setTitle("Orbit Messenger");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        mc.getAllMessages();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
