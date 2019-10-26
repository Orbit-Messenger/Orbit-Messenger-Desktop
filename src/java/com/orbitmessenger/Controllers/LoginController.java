package com.orbitmessenger.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
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
    public void login() {
        String username = this.getTextFieldText(usernameTextField);
        String password = this.getTextFieldText(passwordTextField);
        String server = httpServerTxtCheck(this.getTextFieldText(serverTextField));
        if (!checkInput(username, password, server)) {
            int statusCode = Unirest.get(server + "/verifyUser")
                    .basicAuth(username, password).asString().getStatus();
            if (statusCode == 200) {
                MainController mc = new MainController();
                mc.setUsername(username);
                mc.setPassword(password);
                mc.setServer(server);
                System.out.println("PATH: " + this.MAIN_FXML);
                System.out.println("usernameTextField: " + usernameTextField);
                changeSceneTo(this.MAIN_FXML, mc, (Stage) usernameTextField.getScene().getWindow());
            } else {
                // change to a status update
                System.out.println("Couldn't login");
            }
        } else {
            System.out.println("Missing field!");
            popupMissingFieldDialog();
        }
    }

    /**
     * Checks if http:// is entered in the server text field, if not affix it!
     * @return
     */
    public String httpServerTxtCheck(String server) {
        if (server.startsWith("http://")) {
            return server;
        } else {
            return "http://"+server;
        }
    }

    /**
     * Logs in when pressing enter while in any text field!
     */
    @FXML
    public void handleEnterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            login();
        }
    }

    /**
     * Checks to see if the text fields are not empty!
     */
    @FXML
    private boolean checkInput(String username, String password, String server) {
        return (username.isEmpty()) && (password.isEmpty()) && (server.isEmpty());
    }

    /**
     * Popup dialog box displaying missing field!
     */
    @FXML
    private void popupMissingFieldDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error Logging In");
        alert.setHeaderText(null);
        alert.setContentText("Missing Field!");
        alert.showAndWait();
    }
}
