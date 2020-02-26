package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kong.unirest.Unirest;

public class CreateRoomController extends ControllerUtil {

    @FXML
    private VBox mainVBox;
    @FXML
    private TextField roomNameTxtField;
    @FXML
    private Button createRoomBtn;

    private JsonObject properties;

    private String server;
    private String wsServer;

    public String getServer() {
        return server.replace("wss", "https");
    }
    public String getWsServer() { return wsServer = getServer().replace("https", "wss"); }

    public void setServer(String server) {
        this.server = server;
    }

    public void initialize() {
        loadPreferences();

        setDarkMode();
    }

    public void createRoom() {
        System.out.println("Creating Room: " + this.getTextFieldText(roomNameTxtField).trim());
        String roomName = this.getTextFieldText(roomNameTxtField).trim();
        JsonObject roomInfo = new JsonObject();
        roomInfo.addProperty("name", roomName);
        System.out.println("SERVER: " + getServer());
        int statusCode = Unirest.post(  getServer() + "/createRoom")
                .body(roomInfo).asString().getStatus();

        // Closes the window upon creating a room!
        closeCreateRoom();
    }

    private void closeCreateRoom() {
        // get a handle to the stage
        Stage stage = (Stage) createRoomBtn.getScene().getWindow();

        // do what you have to do
        stage.close();
    }

    /**
     * Sends message to server
     * Used by TextArea txtUserMsg to handle Enter key event
     */
    public void handleEnterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            createRoom();
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
                mainVBox.getStylesheets().add(getClass().getResource("../css/" + preferencesObject.get("theme").toString().replace("\"", "")).toString());
            }
        });
    }
}
