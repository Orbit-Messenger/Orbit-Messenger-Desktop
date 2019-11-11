package com.orbitmessenger.Controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.*;

public class PreferencesController extends ControllerUtil {

    @FXML
    private TextField messageNumber;
    @FXML
    public Button savePref;
    @FXML
    private ToggleButton darkTheme;

    private Integer messageNum;
    private Boolean darkThm;

    public class Preferences {
        public Integer messageNumber = 100;
        public Boolean darkTheme = false;
    }

    /**
     * Updates the clients from the preferences screen
     */
    @FXML
    public void savePreferences() {
        System.out.println("Preferences saved!");

        messageNum = convertToInteger(messageNumber.getText().trim());
        darkThm = darkTheme.isSelected();

        // Write JSON file
        writePreferencesToFile();

        closePreferences();
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

    private void writePreferencesToFile() {
        try (Writer writer = new FileWriter(this.PREF_LOC)) {
            Gson gson = new GsonBuilder().create();

            Preferences pref = new Preferences();
            pref.messageNumber = messageNum;
            pref.darkTheme = darkThm;

            String json = gson.toJson(pref);
            gson.toJson(json, writer);
        } catch (IOException e) {
            System.out.println("Error writing JSON Preferences file.");
            e.printStackTrace();
        }
    }

    private void closePreferences() {
//        MainController mc = new MainController();
//        changeSceneTo(this.MAIN_FXML, mc , (Stage) darkTheme.getScene().getWindow());
        // get a handle to the stage
        Stage stage = (Stage) savePref.getScene().getWindow();

        // do what you have to do
        stage.close();
    }

    private Integer convertToInteger(String messageNumTxt) {
        return Integer.parseInt(messageNumTxt);
    }
}
