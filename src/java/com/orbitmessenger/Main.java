package com.orbitmessenger;

import com.orbitmessenger.Controllers.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("./FXML/loginView.fxml"));
        LoginController loginController = new LoginController();
        loader.setController(loginController);
        Parent root = (Parent) loader.load();
        primaryStage.getIcons().add(new Image("com/orbitmessenger/images/orbit.png"));
        primaryStage.setTitle("Orbit Messenger");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
