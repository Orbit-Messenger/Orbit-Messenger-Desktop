package com.orbitmessenger.Controllers;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import kong.unirest.Unirest;
import org.controlsfx.control.ToggleSwitch;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Type;

public class PreferencesController extends ControllerUtil {

    @FXML
    private VBox mainVBox;
    @FXML
    private TextField messageNumberTxtField;
    @FXML
    private Button savePrefBtn;
    @FXML
    private ToggleSwitch darkThemeToggleBtn;
    @FXML
    TextField usernameTextField;
    @FXML
    TextField passwordTextField;

    private String server, wsServer, username;

    public String getWsServer() { return wsServer = server.replace("https", "wss"); }

    public PreferencesController(String server, String username){
       this.server = server;
       this.username = username;
    }

    public void initialize() {
        loadPreferences();
        setDarkMode();
        messageNumberTxtField.setText(PreferencesObject.get("messageNumber").toString());
        darkThemeToggleBtn.setSelected(PreferencesObject.get("darkTheme").getAsBoolean());
    }
    /**
     * Updates the clients from the preferences screen
     */
    @FXML
    public void changePassword() {
        String password = passwordTextField.getText();
        JsonObject json = new JsonObject();
        json.addProperty("username", this.username);
        json.addProperty("password", password);
        int status = Unirest.post(getWsServer() + "/changePassword").body(json).asString().getStatus();
        System.out.println("Password change: " + status);
    }

    /**
     * Updates the clients from the preferences screen
     */
    @FXML
    public void savePreferences() {
        if (checkField(messageNumberTxtField.getText().trim())) {
            messageNumber = convertToInteger(messageNumberTxtField.getText().trim());
            darkTheme = darkThemeToggleBtn.isSelected();
            setPreferences();

            // Write JSON file
            writePreferencesToFile();

            System.out.println("Preferences saved!");

            closePreferences();
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

    private void closePreferences() {
        // get a handle to the stage
        Stage stage = (Stage) savePrefBtn.getScene().getWindow();

        // do what you have to do
        stage.close();
    }

    private boolean checkField(String messageNumTxt) {
        try {
            Integer.parseInt(messageNumTxt);
            return true;
        } catch (Exception e) {
            messageNumberTxtField.setStyle("-fx-control-inner-background: red");
            return false;
        }
    }

    private Integer convertToInteger(String messageNumTxt) {
        return Integer.parseInt(messageNumTxt);
    }
}
