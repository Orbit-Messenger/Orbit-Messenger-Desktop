package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import javax.swing.text.MaskFormatter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MainController extends ControllerUtil {

    private String username, password, server;
    private JsonObject properties;
    private ArrayList<Integer> messageIds = new ArrayList<>();

    @FXML
    private ListView messagesListView;
    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private Button btnSend;
    @FXML
    ListView userListView;
    @FXML
    private TextArea messageTextArea;

    private ObservableList<String> users;

    // Server Configuration
    private boolean connected;

    private WSClient wsClient;

    public void initialize() throws URISyntaxException {
        wsClient = new WSClient(new URI(this.getServer() + "/"), getUsername());
        wsClient.connect(); // creates the websocket connection
        updateHandler.start(); // Starts the update handler thread
    }

    private String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    private JsonObject getProperties() { return properties; }

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }

    /**
     * Handles all the UI updating when json is received from the websocket
     */
    private Thread updateHandler = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    JsonObject serverMessage = wsClient.getServerResponse();
                    if(serverMessage != null) {
                        updateMessages(getMessagesFromJsonObject(serverMessage));
                        updateUsers(getUsersFromJsonObject(serverMessage));
                        if(serverMessage.has("delete")) {
                            deleteMessageLocally(serverMessage.get("messageId").getAsInt());
                        }
                    }
                    Thread.sleep(500); // Milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    /**
     * Handles when to read the Preferences Json file
     */
    private Thread updatePreferences = new Thread(new Runnable() {
        @Override
        public void run() {
            while (getAllShowingStages().size() > 1) {
                try {
                    Thread.sleep(1000); // Milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            readPreferencesFile();
        }
    });

    /**
     * Gets the messages index from the json object passed to it
     */
    private JsonArray getMessagesFromJsonObject(JsonObject serverResponse){
        if(serverResponse.has("messages")){
            return serverResponse.getAsJsonArray("messages");
        }
        return null;
    }

    /**
     * Gets the activeUser index from the json object passed to it
     */
    private JsonArray getUsersFromJsonObject(JsonObject serverResponse){
        String jsonKey = "activeUsers";
        if(serverResponse.has(jsonKey)){
            try{
                return serverResponse.getAsJsonArray(jsonKey);
            } catch (Exception e){
                return null;
            }
        }
        return null;
    }

    /**
     * Sends message to server
     * Used by TextArea txtUserMsg to handle Enter key event
     */
    public void handleEnterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
        }
    }

    /**
     * To send a message to the server
     */
    public void deleteMessage(String id) {
        JsonObject submitMessage = wsClient.createSubmitObject(
                "delete",
                id,
                getUsername(),
                getProperties()
        );

        System.out.println("Message to delete: " + submitMessage.toString());

        wsClient.send(submitMessage.toString().trim());
    }

    /**
     * Sends the updated properties
     */
    public void sendProperties() {
        JsonObject propertiesMessage = wsClient.createSubmitObject(
                null,
                null,
                getUsername(),
                getProperties()
        );

        System.out.println("Properties to send: " + propertiesMessage.toString());

        wsClient.send(propertiesMessage.toString().trim());
    }

    /**
     * To send a message to the server
     */
    public void sendMessage() {
        String userText = messageTextArea.getText().trim();
        if(userText.isEmpty()) {
            return;
        }

        JsonObject submitMessage = wsClient.createSubmitObject(
                "add",
                userText,
                getUsername(),
                null
        );

        System.out.println("Message to send: " + submitMessage.toString());

        wsClient.send(submitMessage.toString().trim());
        messageTextArea.setText("");
        messageTextArea.requestFocus();
    }

    /**
     * To send a message to the console or the GUI
     */
    public void updateMessages(JsonArray newMessages) {
        if(newMessages == null){
            return;
        }
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (Object message : newMessages) {
            JsonObject m = (JsonObject) message;

            // Takes the messageId from the server and assigns it to the global messageIds
            // we'll use for the delete function.
            messageIds.add(m.get("messageId").getAsInt());

            messageBoxes.add(
                    createMessageBox(
                            m.get("username").toString().replace("\"", ""),
                            m.get("message").toString().replace("\"", ""))
            );
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                messagesListView.getItems().addAll(messageBoxes);
            }
        });

        // Scrolls to the bottom
        scrollToBottom();
    }

    public String trimUsers(String user) {
        return user.replace("\"", "");
    }

    public void updateUsers(JsonArray users) {
        if(users == null){
            return;
        }
        ArrayList<Label> userLabels = new ArrayList<>();
        for (Object user : users) {
            Label label = new Label();
            label.setText(trimUsers(user.toString()));
            userLabels.add(label);
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userListView.getItems().clear();
                userListView.getItems().addAll(userLabels);
            }
        });

        // Scrolls to the bottom
        scrollToBottom();
    }

    /**
     * Creates a message box with proper formatting
     */
    private VBox createMessageBox(String username, String message) {
        VBox vbox = new VBox();
        // check if username == the current user or moves messages to the right
        if (!username.equals(this.getUsername())) {
            vbox.setAlignment(Pos.CENTER_RIGHT);
        }
        vbox.setStyle(".messageBox");
        vbox.getStyleClass().add("messageBox");
        Label usernameLabel = new Label();
        Label messageLabel = new Label();
        usernameLabel.setText(username);
        messageLabel.setText(message);
        vbox.getChildren().add(usernameLabel);
        vbox.getChildren().add(messageLabel);
        return vbox;
    }


    /**
     * Popup dialog box displaying About information!
     */
    public void popupAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Orbit Messenger");
        alert.setHeaderText(null);
        String line2 = ("Designed and built by Brody and Maxwell in Utah!" + "\n" + "https://github.com/MaxwellM/Orbit-Messenger");
        // create a hyperlink
        Hyperlink hyperlink = new Hyperlink("hyperlink");
        alert.setContentText(line2);
        alert.showAndWait();
    }

    /**
     * Opens the preferences window
     */
    public void openPreferences() {
        System.out.println("Opening Preferences!");
        PreferencesController pref = new PreferencesController();
        pref.changeSceneTo(this.PREF_FXML, pref , new Stage());

        updatePreferences.start();
    }

    public ObservableList<Stage> getAllShowingStages() {
        ObservableList<Stage> stages = FXCollections.observableArrayList();
        Window.getWindows().forEach(w -> {
            if (w instanceof Stage) {
                stages.add((Stage) w);
            }
        });
        return stages;
    }

    /**
     * Switch back to the login scene
     */
    public void switchToLogin() {
        // Stop timer
        //wsClient.stopIntervalForPing();
        // Switches back to the Login Controller/Window
        LoginController login = new LoginController();
        login.changeSceneTo(this.LOGIN_FXML, login, (Stage) messageTextArea.getScene().getWindow());
    }

    /**
     * Scrolls to the chat window to the bottom.
     * Preferable when there are new messages.
     */
    public void scrollToBottom() {
        messagesScrollPane.vvalueProperty().bind(messagesListView.heightProperty());
    }

    /**
     * Sends a request to the server to delete a message by sending the messageId.
     */
    public void selectMessageToDelete() {
        final int selectedId  = messagesListView.getSelectionModel().getSelectedIndex();
        System.out.println("Index: " + selectedId);
        deleteMessage(messageIds.get(selectedId).toString());

        // Wait for success message
        // messagesListView.getItems().remove(selectedId);
    }

    /**
     * Deletes a message locally by messageId sent from the server.
     * @param messageId
     */
    public void deleteMessageLocally(Integer messageId) {
        final int selectedId = messageIds.indexOf(messageId);
        messagesListView.getItems().remove(selectedId);
    }

    /**
     * Reads the Preferences file
     */
    public void readPreferencesFile() {
        PreferencesController pc = new PreferencesController();
        Object ref = pc.readPreferencesFile();
        properties = (JsonObject) new JsonParser().parse(ref.toString());
        sendProperties();
    }

    /**
     * Closes the program
     */
    public void closeProgram() {
        System.out.println("Calling Platform.exit():");
        Platform.exit();
        System.out.println("Calling System.exit(0):");
        System.exit(0);
    }
}
