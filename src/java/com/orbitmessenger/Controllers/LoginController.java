package com.orbitmessenger.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import kong.unirest.Unirest;

public class LoginController extends ControllerUtil {
    @FXML
    Button loginButton;

    @FXML
    Button createUserButton;

    @FXML
    TextField usernameTextField;
    @FXML
    TextField passwordTextField;
    @FXML
    TextField serverTextField;

    @FXML
    public void login(){
       String username = this.getTextFieldText(usernameTextField);
       String password = this.getTextFieldText(passwordTextField);
        String server = this.getTextFieldText(serverTextField);
       int statusCode = Unirest.get(server + "/verifyUser")
               .basicAuth(username, password).asString().getStatus();
       if(statusCode == 200){
            MainController mc = new MainController();
            mc.setUsername(username);
            mc.setPassword(password);
            mc.setServer(server);
           changeSceneTo(this.MAIN_FXML, mc, usernameTextField);
       } else{
           // change to a status update
           System.out.println("Couldn't login");
       }
    }
}
