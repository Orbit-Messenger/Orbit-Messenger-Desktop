package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kong.unirest.Unirest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class PreferencesController extends ControllerUtil {

    @FXML
    private VBox mainVBox;
    @FXML
    private TextField messageNumberTxtField;
    @FXML
    private CheckBox groupMessagesCheckBox;
    @FXML
    private Button savePrefBtn;
    @FXML
    TextField usernameTextField;
    @FXML
    TextField passwordTextField;
    @FXML
    ChoiceBox themeChoicesDropdown;
    @FXML
    TextField fontSizeTxtField;
    @FXML
    Button profilePictureButton;
    @FXML
    HBox profilePictureHBox;

    private String server, wsServer, username;
    private ArrayList<String> cssChoices = new ArrayList<String>();
    private Stage mainStage;

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
        groupMessagesCheckBox.setSelected(PreferencesObject.get("groupMessages").getAsBoolean());
        fontSizeTxtField.setText(PreferencesObject.get("fontSize").toString());
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
            groupMessages = groupMessagesCheckBox.isSelected();
            fontSize = Integer.valueOf(fontSizeTxtField.getText());

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

    @FXML
    public void closePreferences() {
        // get a handle to the stage
        Stage stage = (Stage) savePrefBtn.getScene().getWindow();

        // do what you have to do
        stage.close();
    }

    @FXML
    public void uploadProfilePicture() {
        mainStage = (Stage) mainVBox.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        System.out.println(selectedFile.getName());
        System.out.println(selectedFile.getTotalSpace());
        if (selectedFile != null) {
            //mainStage.display(selectedFile);

            // Send file to the server!
            if (checkFile(selectedFile)) {
                int statusCode;
                try{
//                    statusCode = Unirest.post(this.getServer() + "/addAvatar")
//                            .field("avatar", selectedFile)
//                            .field("username", this.username).asJson().getStatus();
                    InputStream file = new FileInputStream(selectedFile);
                    statusCode = Unirest.post(this.getServer() + "addAvatar")
                            .field("file", file, selectedFile.getName())
                            .asEmpty().getStatus();
                } catch (Exception e){
                    System.out.println("Couldn't upload AVATAR!");
                    return;
                }
                //if(10<x && x<20)
                if (200 <= statusCode && statusCode < 300) {
                    System.out.println("Avatar loaded!");
                } else {
                    System.out.println("Looks like it didn't work!");
                }

//                HttpResponse<JsonNode> jsonResponse = Unirest.post(
//                        this.getServer()+"addAvatar")
//                        .field("file", selectedFile)
//                        .asJson();
//                ;
                //assertEquals(201, jsonResponse.getStatus());
            } else {
                //
                System.out.println("Didn't select AVATAR...");
            }
        }
    }

    @FXML
    public void getInt() {
        // Lets get the absolute value of the number and make sure it is within a certain bounds
        int absoluteValue = 250;
        try {
            absoluteValue = Math.abs(convertToInteger(messageNumberTxtField.getText().trim()));
        } catch (Exception e) {
            System.out.println("Error converting to Int, setting to a default value." + e.toString());
            messageNumberTxtField.setText(String.valueOf(absoluteValue));
        }
        if (absoluteValue > 1000) {
            messageNumberTxtField.setText("1000");
        } else if (absoluteValue < 1) {
            messageNumberTxtField.setText("1");
        } else {
            // Nothing! Its good!
        }
    }

    private boolean checkFile(File file) {
        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("creationTime     = " + attr.creationTime());
//        System.out.println("lastAccessTime   = " + attr.lastAccessTime());
//        System.out.println("lastModifiedTime = " + attr.lastModifiedTime());
//
//        System.out.println("isDirectory      = " + attr.isDirectory());
//        System.out.println("isOther          = " + attr.isOther());
//        System.out.println("isRegularFile    = " + attr.isRegularFile());
//        System.out.println("isSymbolicLink   = " + attr.isSymbolicLink());
//        System.out.println("size             = " + attr.size());

        // Now, we must determine that the file is of type .jpg and isn't too large.
        if (attr.size() < 250000) {
            return true;
        } else {
            System.out.println("File too big");
            profilePictureHBox.setStyle("-fx-border-color: red");
            return false;
        }
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
                //System.out.println("File " + listOfFiles[i].getName());
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
