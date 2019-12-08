package com.orbitmessenger.Controllers;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import kong.unirest.Unirest;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;

public class PreferencesController extends ControllerUtil {

    @FXML
    private VBox mainVBox;
    @FXML
    private TextField messageNumberTxtField;
    @FXML
    private Button savePrefBtn;
    @FXML
    TextField usernameTextField;
    @FXML
    TextField passwordTextField;
    @FXML
    ChoiceBox themeChoicesDropdown;

    private String server, wsServer, username;
    private ArrayList<String> cssChoices = new ArrayList<String>();

    public String getWsServer() { return wsServer = server.replace("https", "wss"); }
    public String getServer() { return server.replace("wss", "https"); }

    public PreferencesController(String server, String username){
       this.server = server;
       this.username = username;
    }

    public void initialize() {
        loadPreferences();
        readCSSFiles();
        setDarkMode();
        messageNumberTxtField.setText(PreferencesObject.get("messageNumber").toString());
        themeChoicesDropdown.getSelectionModel().select(cssChoices.indexOf(PreferencesObject.get("theme").toString().replace("\"", "")));
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
        int status = Unirest.post(getServer() + "/changePassword").body(json).asString().getStatus();
        System.out.println("Password change: " + status);
    }

    /**
     * Updates the clients from the preferences screen
     */
    @FXML
    public void savePreferences() {
        if (checkField(messageNumberTxtField.getText().trim())) {
            messageNumber = convertToInteger(messageNumberTxtField.getText().trim());
            theme = themeChoicesDropdown.getSelectionModel().getSelectedItem().toString();
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
                mainVBox.getStylesheets().clear();
                mainVBox.getStylesheets().add(getClass().getResource("../css/" + themeChoicesDropdown.getSelectionModel().getSelectedItem()).toString().replace("\"", ""));
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

    private void readCSSFiles() {
        File folder = new File("src/java/com/orbitmessenger/css");
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                cssChoices.add(listOfFiles[i].getName());
                System.out.println("File " + listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        themeChoicesDropdown.setItems((FXCollections.observableArrayList(cssChoices)));
    }

    private Integer convertToInteger(String messageNumTxt) {
        return Integer.parseInt(messageNumTxt);
    }
}
