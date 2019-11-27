package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kong.unirest.Unirest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoginController extends ControllerUtil {

    @FXML
    Label statusBarLabel;
    @FXML
    VBox mainVBox;
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
    CheckBox sslCheckBox;

    private JsonObject properties;

    public void initialize() throws URISyntaxException {
        loadPreferences();
        setDarkMode();
    }
    
    public String readCert() {
        String filePath = "./localhost.crt";
        String content = "";

        try
        {
            content = Files.readString(Paths.get(filePath));

            System.out.println(content);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return content;
    }


    @FXML
    public void login() {
        String username = this.getTextFieldText(usernameTextField).trim();
        String password = this.getTextFieldText(passwordTextField).trim();
        String server = this.getTextFieldText(serverTextField).trim();
        Boolean ssl = this.sslCheckBox.isSelected();
        String serverPrefix = httpServerTxtCheck(server);
        String cert = readCert();
        if (!checkInput(username, password, server)) {
            JsonObject loginInfo = new JsonObject();
            loginInfo.addProperty("username", username);
            loginInfo.addProperty("password", password);
            loginInfo.addProperty("ssl", ssl);
            loginInfo.addProperty("cert", cert);
            int statusCode;
            try{
                statusCode = Unirest.post(serverPrefix + "/verifyUser").body(loginInfo).asString().getStatus();
            } catch (Exception e){
                sendStatusBarError("Couldn't connect to server: " + e.toString());
                return;
            }
            if (statusCode == 200) {
                MainController mc = new MainController();
                mc.setUsername(username);
                mc.setPassword(password);
                mc.setServer(serverPrefix);
                mc.setSSL(ssl);
                changeSceneTo(this.MAIN_FXML, mc, (Stage) usernameTextField.getScene().getWindow());
            } else {
                // change to a status update
                System.out.println("Couldn't login");
            }
        } else {
            popupMissingFieldDialog();
        }
    }

    @FXML
    public void createUser() {
        String username = this.getTextFieldText(usernameTextField).trim();
        String password = this.getTextFieldText(passwordTextField).trim();
        String server = this.getTextFieldText(serverTextField).trim();
        String serverPrefix = httpServerTxtCheck(server);
        if (!checkInput(username, password, server)) {
            JsonObject loginInfo = new JsonObject();
            loginInfo.addProperty("username", username);
            loginInfo.addProperty("password", password);
            int statusCode = Unirest.post(serverPrefix + "/createUser")
                    .body(loginInfo).asString().getStatus();
            if (statusCode == 200) {
                MainController mc = new MainController();
                mc.setUsername(username);
                mc.setPassword(password);
                mc.setServer(serverPrefix);
                changeSceneTo(this.MAIN_FXML, mc, (Stage) usernameTextField.getScene().getWindow());
            } else {
                // change to a status update
                System.out.println("Couldn't login");
            }
        } else {
            popupMissingFieldDialog();
        }
    }

    /**
     * Toggles Dark Mode based upon the properties Object, obtained from the properties.json file.
     */
    private void setDarkMode() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (PreferencesObject.get("darkTheme").getAsBoolean()) {
                    mainVBox.getStylesheets().remove(getClass().getResource("../css/ui.css").toString());
                    mainVBox.getStylesheets().add(getClass().getResource("../css/darkMode.css").toString());
                } else {
                    mainVBox.getStylesheets().remove(getClass().getResource("../css/darkMode.css").toString());
                    mainVBox.getStylesheets().add(getClass().getResource("../css/ui.css").toString());
                }
            }
        });
    }

    /**
     * Checks if http:// is entered in the server text field, if not affix it!
     * @return
     */
    public String httpServerTxtCheck(String server) {
        if (server.startsWith("https://")) {
            return server;
        } else {
            return "https://"+server;
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
        return username.isEmpty() || (password.isEmpty()) || (server.isEmpty());
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

    private void sendStatusBarError(String message){
        statusBarLabel.setText(message);
        statusBarLabel.setStyle("-fx-background-color: red;");
    }
}
