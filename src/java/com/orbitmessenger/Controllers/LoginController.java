package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kong.unirest.Unirest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
    ComboBox serverComboBox;

    private JsonObject properties;

    final String SERVER_LIST ="src/java/com/orbitmessenger/preferences/servers.json";

    ArrayList<String> serverList = new ArrayList<>();

    public void initialize() throws URISyntaxException {
        loadPreferences();
        setDarkMode();
        addServersToComboBox();
    }

    @FXML
    public void login() {
        String username = this.getTextFieldText(usernameTextField).trim();
        String password = this.getTextFieldText(passwordTextField).trim();
        String server = serverComboBox.getSelectionModel().getSelectedItem().toString().trim();
        String serverPrefix = httpServerTxtCheck(server);
        if (!checkInput(username, password, server)) {
            JsonObject loginInfo = new JsonObject();
            loginInfo.addProperty("username", username);
            loginInfo.addProperty("password", password);
            //loginInfo.addProperty("cert", cert);
            int statusCode;
            try{
                statusCode = Unirest.post(serverPrefix + "/verifyUser").body(loginInfo).asString().getStatus();
            } catch (Exception e){
                sendStatusBarError("Couldn't connect to server: " + e.toString());
                return;
            }
            if (statusCode == 200) {
                addServerToServerListIfNotExists(server);
                MainController mc = new MainController();
                ClientInfo clientInfo = new ClientInfo(username, password, serverPrefix, "general");
                mc.setClientInfo(clientInfo);
                // We do this so that when someone clicks the RED X it will call the closeProgram method.
                // This is a roundabout way. You can thank Java for that.
                mc.setClose((Stage) usernameTextField.getScene().getWindow());
                changeSceneTo(this.MAIN_FXML, mc, (Stage) usernameTextField.getScene().getWindow());
            } else {
                // change to a status update
                System.out.println("Couldn't login");
            }
        } else {
            popupMissingFieldDialog();
        }
    }

    // adds the servers from the server_list to the combo box on the UI
    private void addServersToComboBox(){
        try {
            // creates a buffer for all the file data and converts the data to a string
            BufferedReader br = new BufferedReader(new FileReader(SERVER_LIST));
            String jsonFileData = "";
            String placeHolder;
            while((placeHolder = br.readLine()) != null){
                jsonFileData += placeHolder;
            }
            br.close();
            // creates a json parser to parse the string into a jsonObject and then converts that into a jsonArray
            JsonParser parser = new JsonParser();
            JsonArray listOfServers = parser.parse(jsonFileData).getAsJsonObject().get("servers").getAsJsonArray();

            // adds each server to the combo box
            for(JsonElement server : listOfServers){
                serverList.add(server.toString().replace("\"", ""));
            }
            serverComboBox.getItems().addAll(serverList);
            // sets the combo box to the first element
            serverComboBox.getSelectionModel().select(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // adds a new server to the server.json file from the serverList ArrayList
    private void addServerToServerListIfNotExists(String server){
        if(!serverList.contains(server)){
            serverList.add(server);
        }
        JsonArray servers = new JsonArray();
        for (String s: serverList){
            servers.add(s);
        }
        JsonObject serverListJson = new JsonObject();
        serverListJson.add("servers", servers);
        try {
            FileWriter writer = new FileWriter(SERVER_LIST);
            writer.write(serverListJson.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    public String wssServerChange(String server) {
        return server.replaceFirst("https", "wss");
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

    @FXML
    public void createUser() {
        String username = this.getTextFieldText(usernameTextField).trim();
        String password = this.getTextFieldText(passwordTextField).trim();
        String server = serverComboBox.getSelectionModel().getSelectedItem().toString().trim();
        String serverPrefix = httpServerTxtCheck(server);
        if (!checkInput(username, password, server)) {
            JsonObject loginInfo = new JsonObject();
            loginInfo.addProperty("username", username);
            loginInfo.addProperty("password", password);
            int statusCode = Unirest.post(serverPrefix + "/createUser")
                    .body(loginInfo).asString().getStatus();
            if (statusCode == 200) {
                MainController mc = new MainController();
                ClientInfo clientInfo = new ClientInfo(username, password, serverPrefix, "general");
                mc.setClientInfo(clientInfo);
                changeSceneTo(this.MAIN_FXML, mc, (Stage) usernameTextField.getScene().getWindow());
            } else {
                // change to a status update
                System.out.println("Couldn't login");
            }
        } else {
            popupMissingFieldDialog();
        }
    }
}
