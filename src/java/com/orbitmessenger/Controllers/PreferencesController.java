package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    @FXML
    Label propertiesLabel;

    private String server, wsServer, username, password;
    private ArrayList<String> cssChoices = new ArrayList<String>();
    private Stage mainStage;

    public String getWsServer() { return wsServer = server.replace("https", "wss"); }
    public String getServer() { return server.replace("wss", "https"); }

    public PreferencesController(String server, String username, String password){
       this.server = server;
       this.username = username;
       this.password = password;
    }

    public void initialize() {
        loadPreferences();
        readCSSFiles();
        setDarkMode();
        messageNumberTxtField.setText(preferencesObject.get("messageNumber").toString());
        themeChoicesDropdown.getSelectionModel().select(cssChoices.indexOf(preferencesObject.get("theme").toString().replace("\"", "")));
        groupMessagesCheckBox.setSelected(preferencesObject.get("groupMessages").getAsBoolean());
        fontSizeTxtField.setText(preferencesObject.get("fontSize").toString());
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
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
//        System.out.println(selectedFile.getName());
//        System.out.println(selectedFile.getTotalSpace());
        if (selectedFile != null) {
            // Send file to the server!
            if (checkFile(selectedFile)) {
                int statusCode;
                try{
                    InputStream file = new FileInputStream(selectedFile);
                    statusCode = Unirest.post(this.getServer() + "/addAvatar")
                            .basicAuth(this.username, this.password)
                            .field("file", file, selectedFile.getName())
                            .asEmpty().getStatus();
                } catch (Exception e){
                    sendError("Couldn't upload Avatar!: " + e.toString());
                    System.out.println("Couldn't upload AVATAR! " + e.toString());
                    return;
                }
                if (200 <= statusCode && statusCode < 300) {
                    sendAcceptance("Avatar Accepted!");
                } else {
                    sendError("The server didn't accept your image!");
                }
            } else {
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
            sendError(e.toString());
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
            sendError("File too big!");
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

    private void setPropertiesLabelText(String text){
        propertiesLabel.setText(text);
    }

    private  void sendError(String error){
        propertiesLabel.setStyle("-fx-background-color: red");
        setPropertiesLabelText(error);
    }

    private  void sendAcceptance(String acceptMessage){
        propertiesLabel.setStyle("-fx-background-color: yellowGreen");
        setPropertiesLabelText(acceptMessage);
    }
}
