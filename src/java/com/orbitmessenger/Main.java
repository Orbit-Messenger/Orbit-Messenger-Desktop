package com.orbitmessenger;

import com.orbitmessenger.Controllers.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        //DEBUG Use this to skip logging in and comment out other FXML code!
//        FXMLLoader loader = new FXMLLoader(getClass().getResource("./FXML/mainView.fxml"));
//        MainController mainController = new MainController();
//        mainController.setUsername("brody");
//        mainController.setPassword("test");
//        mainController.setServer("http://localhost:3000/");
//        loader.setController(mainController);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("./FXML/loginView.fxml"));
        LoginController loginController = new LoginController();
        loader.setController(loginController);

        Parent root = (Parent) loader.load();
        primaryStage.getIcons().add(new Image("com/orbitmessenger/images/orbit.png"));
        primaryStage.setTitle("Orbit Messenger");
        primaryStage.setScene(new Scene(root));
        // This will call the closeProgram() function in MainController so it closes correctly when
        // clicking on the red X!
        //primaryStage.setOnHidden(e -> LoginController.closeProgram());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
