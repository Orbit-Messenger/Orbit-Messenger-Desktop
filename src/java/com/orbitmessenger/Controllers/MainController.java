package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainController extends ControllerUtil {

    private String username, password, server, currentRoom;
    private String wsServer, httpsServer;
    private Boolean ssl;
    private JsonObject properties;
    private ArrayList<Integer> messageIds = new ArrayList<>();

    @FXML
    private VBox mainVBox = new VBox();
    @FXML
    private ListView messagesListView;
    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private Button btnSend;
    @FXML
    private ListView userListView;
    @FXML
    private ListView roomListView;
    @FXML
    private TextArea messageTextArea;
    @FXML
    private Label roomLabel;
    @FXML
    private ObservableList<String> users;

    // Server Configuration
    private boolean connected;

    private WSClient wsClient;

    private Thread wsConnectionThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!wsClient.isOpen()) {
                    wsClient.reconnect();
            }
        }
    });

    @FXML
    public void initialize() throws URISyntaxException {
        wsClient = new WSClient(new URI(this.getServer()), getUsername());

        wsClient.setConnectionLostTimeout( 60 );

        wsConnectionThread.start();
        try {
            wsConnectionThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Server Settings: " + wsClient.getConnection().toString());

        updateHandler.start(); // Starts the update handler thread
        loadPreferences();
        //sendProperties();
        setDarkMode();
        roomLabel.setText("general");
        currentRoom = roomLabel.getText();
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

    public String getServer() { return server; }
    public String getWsServer() { return wsServer = server.replace("https", "wss"); }

    public void setServer(String server) {
        this.server = server;
    }

    //private JsonObject getProperties() { return properties; }

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }

    /**
     * Handles all the UI updating when json is received from the websocket
     */
    private Thread updateHandler = new Thread(new Runnable() {
        @Override
        public void run() {
            // Synchronized will wait for serverResponse in WSClient to be released.
            // It gets released when onMessage returns.
            // This allows us to dynamically update when onMessage returns. This way we won't need to
            // continuously loop and check. When .wait() is called this thread will BLOCK until .notfiy() is called.
            synchronized (wsClient.monitor) {
                while (true) {
                    while (wsClient.size() == 0) {
                        try {
                            // This means to wait for serverResponse to be filled via onMessage.
                            wsClient.monitor.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Error: " + e.toString());
                        }
                    }
                    try {
                        JsonObject serverMessage = wsClient.getServerResponse();
                        if (serverMessage != null) {
                            System.out.println("Response: " + serverMessage);
                            if (serverMessage.has("messages")) {
                                updateMessages(getMessagesFromJsonObject(serverMessage));
                            }
                            if (serverMessage.has("activeUsers")) {
                                updateUsers(getUsersFromJsonObject(serverMessage));
                            }
                            if (serverMessage.has("chatrooms")) {
                                updateRooms(getRoomsFromJsonObject(serverMessage));
                            }
                            if (serverMessage.has("action")) {
                                deleteMessageLocally(serverMessage.get("messageId").getAsInt());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        switchToLogin();
                        return;
                    }
                }
            }
        }
    });

    public void setClose(Stage stage) {
        // This will call the closeProgram() function in MainController so it closes correctly when
        // clicking on the red X!
        stage.setOnHidden(e -> closeProgram());
    }

    /**
     * Gets the messages index from the json object passed to it
     */
    private JsonArray getMessagesFromJsonObject(JsonObject serverResponse) {
        if (serverResponse.has("messages")) {
            return serverResponse.getAsJsonArray("messages");
        }
        return null;
    }

    /**
     * Gets the activeRoom index from the json object passed to it
     */
    private JsonArray getRoomsFromJsonObject(JsonObject serverResponse) {
        String jsonKey = "chatrooms";
        if (serverResponse.has(jsonKey)) {
            try {
                return serverResponse.getAsJsonArray(jsonKey);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the activeUser index from the json object passed to it
     */
    private JsonArray getUsersFromJsonObject(JsonObject serverResponse) {
        String jsonKey = "activeUsers";
        if (serverResponse.has(jsonKey)) {
            try {
                return serverResponse.getAsJsonArray(jsonKey);
            } catch (Exception e) {
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
    public void deleteMessage(int index) {
        JsonObject submitMessage = wsClient.createSubmitObject(
                "delete",
                null,
                messageIds.get(index).toString(),
                getUsername(),
                getPreferences()
        );
        wsClient.send(submitMessage.toString().trim());
    }

    /**
     * Sends the updated properties
     */
    public void sendProperties() {
        JsonObject propertiesMessage = wsClient.createSubmitObject(
                "properties",
                currentRoom,
                null,
                getUsername(),
                getPreferences()
        );
        wsClient.send(propertiesMessage.toString().trim());
    }

    /**
     * To send a message to the server
     */
    public void sendMessage() {
        String userText = messageTextArea.getText().trim();
        if (userText.isEmpty()) {
            return;
        }

        JsonObject submitMessage = wsClient.createSubmitObject(
                "add",
                currentRoom,
                userText,
                getUsername(),
                properties
        );
        wsClient.send(submitMessage.toString().trim());
        messageTextArea.setText("");
        messageTextArea.requestFocus();
    }

    private String convertTime(String time) {
        // Time comes in like this: "2019-12-27T15:08:06.016632Z"
        // Since milliseconds come in with trailing 0's trimmed, let just remove them..
        int lastIndex = time.lastIndexOf('.');
        String trimmedTime = time.substring(0, lastIndex);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date = null;
        try {
            date = sdf.parse(trimmedTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault()).format(date);
    }

    /**
     * To send a message to the console or the GUI
     */
    public void updateMessages(JsonArray newMessages) {
        if (newMessages == null) {
            return;
        }
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (Object message : newMessages) {
            JsonObject m = (JsonObject) message;

            // Takes the messageId from the server and assigns it to the global messageIds
            // we'll use for the delete function.
            messageIds.add(m.get("messageId").getAsInt());

            // the 0, reverses the order so the latest message is on the bottom. Like a chat should be...
            messageBoxes.add(0,
                    createMessageBox(
                            m.get("username").toString().replace("\"", ""),
                            m.get("timestamp").toString(),
                            m.get("message").toString().replace("\"", ""))
            );
        }
        Platform.runLater(() -> {
            messagesListView.getItems().addAll(messageBoxes);
            trimMessagesToMessageLimit();
            scrollToBottom();
        });
    }

    // Message Limit set in Preferences will determine how many messages we'll see on the screen
    public void trimMessagesToMessageLimit() {
        int listSize = messagesListView.getItems().size();
        int messageLimit = getPreferences().get("messageNumber").getAsInt();
        if (listSize == 0) {
            return;
        }
        messagesListView.getItems().remove(0, listSize-messageLimit);
    }

    public String trimUsers(String user) {
        return user.replace("\"", "");
    }

    public void updateRooms(JsonArray rooms) {
        if (rooms == null) {
            return;
        }
        ArrayList<Label> roomLabels = new ArrayList<>();
        for (JsonElement room : rooms) {
            JsonObject obj = room.getAsJsonObject();
            Label label = new Label();
            label.getStyleClass().add("font-color");
            label.setId("roomLabelID");

            label.setOnMouseClicked(new EventHandler<MouseEvent>(){
                @Override
                public void handle(MouseEvent event){
                    if (event.getClickCount() == 2) {
                        if (label.getText() != currentRoom) {
                            switchRoom(label.getText());
                            currentRoom = label.getText();
                        } else {
                            System.out.println("Not switching room since you chose the same room");
                        }
                    }
                }
            });

            roomListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        Integer roomIndex = roomListView.getFocusModel().focusedIndexProperty().getValue();
                        System.out.println("Room Index: " + roomIndex);
                        if (roomLabels.get(roomIndex).getText() != currentRoom) {
                            switchRoom(roomLabels.get(roomIndex).getText());
                            currentRoom = roomLabels.get(roomIndex).getText();
                        } else {
                            System.out.println("Not switching rooms, you chose the same room.");
                        }
                    }
                }
            });

            label.setText(trimUsers(obj.get("name").toString()));
            roomLabels.add(label);
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                roomListView.getItems().clear();
                roomListView.getItems().addAll(roomLabels);
            }
        });
    }

    public void updateUsers(JsonArray users) {
        if (users == null) {
            return;
        }
        ArrayList<Label> userLabels = new ArrayList<>();
        for (Object user : users) {
            Label label = new Label();
            label.getStyleClass().add("font-color");
            label.setAlignment(Pos.CENTER);
            label.setId("userLabelID");
            label.setText(trimUsers(user.toString()));
            userLabels.add(label);

            // In case you select the label within the list
            label.setOnMouseClicked(new EventHandler<MouseEvent>(){
                @Override
                public void handle(MouseEvent event){
                    if (event.getClickCount() == 2) {
                        if (label.getText() != currentRoom) {
                            switchToDirectMessage(label.getText());
                            currentRoom = label.getText();
                        } else {
                            System.out.println("Not switching room since you chose the same room");
                        }
                    }
                }
            });
        }

        // In case you select the row, but not the label in the list
        userListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Integer userIndex = userListView.getFocusModel().focusedIndexProperty().getValue();
                    System.out.println("User Index: " + userLabels.get(userIndex).getText());
                    switchToDirectMessage(userLabels.get(userIndex).getText());
                    currentRoom = userLabels.get(userIndex).getText();
                }
            }
        });

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userListView.getItems().clear();
                userListView.getItems().addAll(userLabels);
            }
        });
    }

    /**
     * Creates a message box with proper formatting
     */
    private VBox createMessageBox(String username, String timestamp, String message) {
        String shortTime = convertTime(timestamp.replace("\"", ""));

        VBox vbox = new VBox();
        HBox hBox = new HBox();
        // check if username == the current user or moves messages to the right
        if ((!username.equals(this.getUsername())) && (!username.equals("admin"))) {
            vbox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setAlignment(Pos.CENTER_RIGHT);
        } else if (username.equals("admin")){
            // must be admin, we want to center these messages
            vbox.setAlignment(Pos.CENTER);
            hBox.setAlignment(Pos.CENTER);
        }
        vbox.setStyle(".messageBox");
        vbox.getStyleClass().add("messageBox");
        Label usernameLabel = new Label();
        Label timeStampLabel = new Label();
        Label messageLabel = new Label();

        usernameLabel.getStyleClass().add("font-color");
        timeStampLabel.getStyleClass().add("font-color");
        messageLabel.getStyleClass().add("font-color");

        usernameLabel.setText(username);
        timeStampLabel.setText(shortTime);
        messageLabel.setText(message);

        vbox.getChildren().add(usernameLabel);
        vbox.getChildren().add(timeStampLabel);
        hBox.getChildren().addAll(usernameLabel, timeStampLabel);
        vbox.getChildren().add(hBox);
        vbox.getChildren().add(messageLabel);

        // Set timestamp font size
        timeStampLabel.setFont(new Font("Arial", 10));
        timeStampLabel.setPadding(new Insets(0, 0, 0, 10));

        return vbox;
    }


    /**
     * Popup dialog box displaying About information!
     */
    public void popupAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogPane dialogPane = alert.getDialogPane();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                dialogPane.getStylesheets().clear();
                dialogPane.getStylesheets().add(getClass().getResource("../css/" + PreferencesObject.get("theme").toString().replace("\"", "")).toString());
            }
        });
        alert.setTitle("Orbit Messenger");
        alert.setHeaderText(null);
        String line2 = ("Designed and built by Brody and Maxwell in Utah!" + "\n" + "https://github.com/MaxwellM/Orbit-Messenger");
        // create a hyperlink
        Hyperlink hyperlink = new Hyperlink("hyperlink");
        alert.setContentText(line2);
        alert.showAndWait();
    }

    /**
     * Opens the create room window
     */
    public void openCreateRoom() {
        System.out.println("Opening Create Room!");
        CreateRoomController createRoom = new CreateRoomController();
        createRoom.setServer(getServer());
        createRoom.changeSceneTo(this.CROOM_FXML, createRoom, new Stage());

        Thread createRoomThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (getAllShowingStages().size() > 1) {
                    try {
                        Thread.sleep(500); // Milliseconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        createRoomThread.start();
    }

    /**
     * Opens the preferences window
     */
    public void openPreferences() {
        System.out.println("Opening Preferences!");
        PreferencesController pref = new PreferencesController(this.server, this.username);
        pref.changeSceneTo(this.PREF_FXML, pref, new Stage());

        Thread updatePreferences = new Thread(new Runnable() {
            @Override
            public void run() {
                while (getAllShowingStages().size() > 1) {
                    try {
                        Thread.sleep(500); // Milliseconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                loadPreferences();
                sendProperties();
                setDarkMode();
            }
        });
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
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // Closing the websocket by sending message we're on out way to the server!
                JsonObject submitMessage = wsClient.createSubmitObject(
                        "logout",
                        null,
                        "",
                        getUsername(),
                        null
                );
                try {
                    wsClient.send(submitMessage.toString().trim());
                } catch (Exception e){
                    System.out.println("Error sending logout message to the sever: " + e.toString());
                }

                // Need to stop the running threads!
                //pingThread.stop();
                wsConnectionThread.stop();
                // Closing the websocket
                wsClient.close();
                // Stop timer
                //wsClient.stopIntervalForPing();
                // Switches back to the Login Controller/Window
                LoginController login = new LoginController();
                login.changeSceneTo(LOGIN_FXML, login, (Stage) messageTextArea.getScene().getWindow());
            }
        });
    }

    /**
     * Scrolls to the chat window to the bottom.
     * Preferable when there are new messages.
     */
    private void scrollToBottom() {
        // This should move the list view to the last item in it's index...
        messagesListView.scrollTo(messagesListView.getItems().size());
    }

    /**
     * Sends a request to the server to delete a message by sending the messageId.
     */
    public void selectMessageToDelete() {
        int selectedIndex = messagesListView.getSelectionModel().getSelectedIndex();
        deleteMessage(selectedIndex);
    }

    /**
     * Deletes a message locally by messageId sent from the server.
     *
     * @param messageId
     */
    public void deleteMessageLocally(Integer messageId) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                int index = messageIds.indexOf(messageId);
                messagesListView.getItems().remove(index);
                messageIds.remove(index);
            }
        });
    }

    /**
     * Toggles Dark Mode based upon the properties Object, obtained from the properties.json file.
     */
    private void setDarkMode() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                mainVBox.getStylesheets().clear();
                mainVBox.getStylesheets().add(getClass().getResource("../css/" + PreferencesObject.get("theme").toString().replace("\"", "")).toString());
            }
        });
    }

    /**
     * Closes the program
     */
    public void closeProgram() {
        // Closing the websocket by sending message we're on out way to the server!
        JsonObject submitMessage = wsClient.createSubmitObject(
                "logout",
                null,
                "",
                getUsername(),
                null
        );
        wsClient.send(submitMessage.toString().trim());
        System.out.println("Calling Platform.exit():");
        Platform.exit();
        System.out.println("Calling System.exit(0):");
        System.exit(0);
    }

    /**
     * Copies the selected object on the UI
     */
    public void copy() {
        final int selectedId = messagesListView.getSelectionModel().getSelectedIndex();
        // This means no message is selected. If that is true, return.
        if (selectedId == -1) {
            return;
        }
        VBox vBox = (VBox) messagesListView.getSelectionModel().getSelectedItem();
        Label children = (Label) vBox.getChildren().get(1);

        // Our clipboard!
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        content.putString(children.getText());

        clipboard.setContent(content);
    }

    /**
     * Pastes what in your clipboard!
     */
    public void paste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        messageTextArea.setText(clipboard.getString());
    }

    /**
     * Switches to a DIRECT MESSAGE room
     */
    public void switchToDirectMessage(String user) {
        JsonArray usersList = new JsonArray();
        usersList.add(getUsername());
        usersList.add(user);
        JsonObject submitMessage = new JsonObject();
        submitMessage.addProperty("action", "chatroom");
        submitMessage.addProperty("chatroom", "direct_message");
        submitMessage.add("users", usersList);

        System.out.println("Message: " + submitMessage.toString().trim());

        wsClient.send(submitMessage.toString().trim());
        messagesListView.getItems().clear();
        messageIds.clear();
        roomLabel.setText("Direct Message: " + user);
    }

    /**
     * Switches room!
     */
    public void switchRoom(String room) {
        System.out.println("Room: " + room);
        JsonObject submitMessage = wsClient.createSubmitObject(
                "chatroom",
                room,
                null,
                getUsername(),
                properties);
        wsClient.send(submitMessage.toString().trim());
        messagesListView.getItems().clear();
        messageIds.clear();
        roomLabel.setText(room);
    }
}
