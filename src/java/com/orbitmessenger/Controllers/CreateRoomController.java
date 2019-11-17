package com.orbitmessenger.Controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import kong.unirest.Unirest;
import org.controlsfx.control.ToggleSwitch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class CreateRoomController extends ControllerUtil {

    @FXML
    private VBox mainVBox;
    @FXML
    private TextField roomNameTxtField;
    @FXML
    private Button createRoomBtn;

    private JsonObject properties;

    MainController mc;

    public void initialize() {
        settPreferences();
    }

    public void createRoom() {
        System.out.println("Creating Room: " + this.getTextFieldText(roomNameTxtField).trim());
        String roomName = this.getTextFieldText(roomNameTxtField).trim();
        JsonObject roomInfo = new JsonObject();
        roomInfo.addProperty("name", roomName);
        int statusCode = Unirest.post( mc.getServer() + "/createRoom")
                .body(roomInfo).asString().getStatus();
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

    public Object readPreferencesFile() {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(this.PREF_LOC));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Gson gson = new Gson();
        Object json = gson.fromJson(bufferedReader, Object.class);

        System.out.println(json.getClass());
        System.out.println(json.toString());

        return json;
    }

    /**
     * Reads the Preferences file
     */
    public void settPreferences() {
        Object ref = readPreferencesFile();
        properties = (JsonObject) new JsonParser().parse(ref.toString());
        setDarkMode();
    }

    /**
     * Toggles Dark Mode based upon the properties Object, obtained from the properties.json file.
     */
    public void setDarkMode() {
        if (properties.get("darkTheme").getAsBoolean()) {
            mainVBox.getStylesheets().remove(getClass().getResource("../css/ui.css").toString());
            mainVBox.getStylesheets().add(getClass().getResource("../css/darkMode.css").toString());
        } else {
            mainVBox.getStylesheets().remove(getClass().getResource("../css/darkMode.css").toString());
            mainVBox.getStylesheets().add(getClass().getResource("../css/ui.css").toString());
        }
    }
}
