package com.orbitmessenger;

import com.orbitmessenger.Controllers.LoginController;
import com.orbitmessenger.Controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        //DEBUG Use this to skip logging in and comment out other FXML code!
        FXMLLoader loader = new FXMLLoader(getClass().getResource("./FXML/mainView.fxml"));
        MainController mainController = new MainController();
        mainController.setUsername("maxwell");
        mainController.setPassword("test");
        mainController.setServer("ws://localhost:3000");
        loader.setController(mainController);
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("./FXML/loginView.fxml"));
//        LoginController loginController = new LoginController();
//        loader.setController(loginController);

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
