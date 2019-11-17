package com.orbitmessenger.Controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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
        setPreferences();
    }

    public void createRoom() {
        String roomName = this.getTextFieldText(roomNameTxtField).trim();
        JsonObject loginInfo = new JsonObject();
        loginInfo.addProperty("room", roomName);
        int statusCode = Unirest.post( mc.getServer() + "/createroom")
                .body(loginInfo).asString().getStatus();
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

    private void setPreferences() {
        Object localPreferences = readPreferencesFile();
        JsonObject localMessageNum = (JsonObject) new JsonParser().parse(localPreferences.toString());
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
