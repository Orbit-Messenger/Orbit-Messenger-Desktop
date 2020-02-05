package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import kong.unirest.Unirest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

class ClientInfo {
    private String username, password, httpServer, currentRoom, wsServer;

    public ClientInfo(String username, String password, String httpServer, String currentRoom) {
        this.username = username;
        this.password = password;
        this.httpServer = httpServer;
        this.currentRoom = currentRoom;
        this.setWsServer(this.httpServer);
    }

    public String getUsername() {
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

    public String getHttpServer() {
        return httpServer;
    }

    public void setHttpServer(String httpServer) {
        this.httpServer = httpServer;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public String getWsServer() {
        return wsServer;
    }

    public void setWsServer() {
        this.wsServer = this.getHttpServer().replace("https", "wss");
    }

    public void setWsServer(String httpServer) {
        this.wsServer = httpServer.replace("https", "wss");
    }
}


public class MainController extends ControllerUtil {

    private String wsServer, httpsServer;
    private JsonObject properties;
    private ArrayList<Integer> messageIds = new ArrayList<>();
    private ArrayList<String> allUsers = new ArrayList<>();
    private ArrayList<int[]> groupIndexes = new ArrayList<>();
    private ArrayList<VBox> groupedMessageBoxes = new ArrayList<>();
    private HashMap<String,Image> imageMap = new HashMap<>();
    private ClientInfo clientInfo;
    private boolean removeSplash = false;
    public ArrayList<Message> messages = new ArrayList<>();

    @FXML
    private VBox mainVBox = new VBox();
    @FXML
    private ListView messagesListView = new ListView();
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
    private Text connectionInformation;
    @FXML
    private VBox bottomVBox;
    @FXML
    private ObservableList<String> users;
    @FXML
    private Circle latencyCircle;
    @FXML
    private Label versionLabel;

    // Server Configuration
    private boolean connected;

    private WSClient wsClient;
    private Logger logger;

    @FXML
    public void initialize() throws URISyntaxException {
        wsClient = new WSClient(
                new URI(clientInfo.getWsServer()),
                clientInfo.getUsername(),
                clientInfo.getPassword());
        logger = Logger.getLogger("test");
        wsClient.setConnectionLostTimeout( 60 );
        wsClient.setTcpNoDelay(true);
        wsClient.setReuseAddr(true);

        wsConnectionThread.start();
        try {
            wsConnectionThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Server Settings: " + wsClient.getConnection().toString());

        setupConnectionInformation();

        waitForPreferencesThread.start();
        try {
            waitForPreferencesThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        loadVersion();
        setDarkMode();

        createSplashScreen(); // Creates the splash screen!
        updateHandler.start(); // Starts the update handler thread
        connectionInformationThread.start(); // Starts the connection information thread
        roomLabel.setText("general");
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    // +++++++++++++++++++++++ THREADS ++++++++++++++++++++++++++
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
                            logger.warning("Error: " + e.toString());
                        }
                    }
                    try {
                        JsonObject serverMessage = wsClient.getServerResponse();
                        if (serverMessage != null) {
                            //logger.info("Response: " + serverMessage);
                            System.out.println(serverMessage);
                            if (serverMessage.has("allUsers")) {
                                updateUsers(wsClient.getUsersFromJsonObject(serverMessage));
                            }
                            if (serverMessage.has("chatrooms")) {
                                updateRooms(wsClient.getRoomsFromJsonObject(serverMessage));
                            }
                            if (serverMessage.has("updateAvatars")) {
                                getAllImages();
                                resetMessages();
                            }
                            if (serverMessage.has("messages")) {
//                                Thread.sleep(5000);
                                if (!removeSplash) {
                                    removeSplashScreen();
                                    removeSplash = true;
                                }
                                updateMessages(wsClient.getMessagesFromJsonObject(serverMessage));
                            }
                            if (serverMessage.has("action")) {
                                deleteMessages(serverMessage.get("messageId").getAsInt());
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

    // What we want is this. We want the latency to be calculated ONLY when the connection information bar is visible.
    // We want it to send a ping once a second, calculating the latency. If the latency is over a second, it should
    // not have to wait and send another ping. If it is under a second, we'll subtract the latency from a second
    // and fire again, meaning it should send about once a second unless it is longer than a second. Yeah?
    private Thread connectionInformationThread = new Thread(new Runnable() {
        @Override
        public void run() {
            synchronized (wsClient.latencyMonitor) {
                while(true) {
                    while (connectionInformation.isVisible()) {
                        while (wsClient.latency == 0) {
                            try {
                                wsClient.sendAPing();
                                // This means to wait for latency to be calculated via onPong.
                                wsClient.latencyMonitor.wait();
                            } catch (InterruptedException e) {
                                logger.warning("Error: " + e.toString());
                            }
                        }
                        try {
                            if (wsClient.latency < 1000) {
                                Thread.sleep(1000 - wsClient.latency);
                            }
                            setConnectionInformation();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        // Setting it back to 0, ready for it to be calculated again when the user toggles the connection
                        // information bar again.
                        if (wsClient.latency > 0) {
                            wsClient.getLatency();
                        }
                        // We set this to wait so that the onPong can be called again. Otherwise we'll disconnect because
                        // the Java Websocket will not get the pong response to reset the lostConnectionTimer.
                        wsClient.latencyMonitor.wait();
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    });

    private Thread waitForPreferencesThread = new Thread(new Runnable() {
        @Override
        public void run() {
            loadPreferences();
            while (PreferencesObject.get("groupMessages") == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }
    // +++++++++++++++++++++++ UI ACTIONS ++++++++++++++++++++++++++

    /**
     * Creates splash screen!
     */
    private synchronized void createSplashScreen() {
        System.out.println("Creating splash screen!");
        //Loading image from images folder. This is our splash screen GIF!
        ImageView imv = new ImageView();
        int imageWidth = 250;
        int imageHeight = 250;
        imv.setFitWidth(imageWidth);
        imv.setFitHeight(imageHeight);
        try {
            Image image = new Image(MainController.class.getResourceAsStream(
                    "../images/rick.gif"),
                    imageWidth, imageHeight, false, false);
            imv.setImage(image);
        } catch (Exception e) {
            e.printStackTrace();
        }

        VBox vBox = new VBox();
        Label label = new Label("Lading messages! Please wait...");
        label.setFont(new Font("Arial", PreferencesObject.get("fontSize").getAsInt()));
        vBox.getChildren().add(imv);
        vBox.getChildren().add(label);
        vBox.setAlignment(Pos.CENTER);

        messagesListView.getItems().add(vBox);
    }

    /**
     * Removes the splash screen!
     */
    private void removeSplashScreen() {
        // Splash screen should be the first item
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                messagesListView.getItems().remove(0);
            }
        });
    }

    /**
     * Switches to a DIRECT MESSAGE room via the context menu
     */
    public void switchToDirectMessage(String user) {
        JsonArray usersList = new JsonArray();
        usersList.add(clientInfo.getUsername());
        usersList.add(user);
        JsonObject submitMessage = new JsonObject();
        submitMessage.addProperty("action", "chatroom");
        submitMessage.addProperty("chatroom", "direct_messages");
        submitMessage.add("users", usersList);

        wsClient.send(submitMessage.toString().trim());
        messagesListView.getItems().clear();
        messageIds.clear();
        roomLabel.setText("Direct Message: " + user);
    }

    /**
     * Switches room!
     */
    public void switchRoom(String room) {
        logger.info("Room: " + room);
        JsonObject submitMessage = wsClient.createSubmitObject(
                "chatroom",
                room,
                null,
                clientInfo.getUsername(),
                properties);
        wsClient.send(submitMessage.toString().trim());
        messagesListView.getItems().clear();
        messageIds.clear();
        roomLabel.setText(room);
        clientInfo.setCurrentRoom(room);
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
                clientInfo.getUsername(),
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
                clientInfo.getCurrentRoom(),
                null,
                clientInfo.getUsername(),
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
                clientInfo.getCurrentRoom(),
                userText,
                clientInfo.getUsername(),
                properties
        );
        wsClient.send(submitMessage.toString().trim());
        messageTextArea.setText("");
        messageTextArea.requestFocus();
    }

    /**
     * Sends a request to the server to delete a message by sending the messageId.
     */
    public void selectMessageToDelete() {
        int selectedIndex = messagesListView.getSelectionModel().getSelectedIndex();
        deleteMessage(selectedIndex);
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
     * Joins a room via the context menu
     */
    public void join() {
        final int selectedId = roomListView.getSelectionModel().getSelectedIndex();
        if (selectedId == -1) {
            return;
        }
        Label label = (Label) roomListView.getItems().get(selectedId);

        System.out.println("Current Room 1: " + clientInfo.getCurrentRoom());
        System.out.println("Current Room 2: " + label.getText());

        // Don't switch if you're already in that room
        if (!clientInfo.getCurrentRoom().equals(label.getText())) {
            switchRoom(label.getText());
        }
    }

    /**
     * Switches to directMessenging via the label you clicked on!
     */
    public void switchToDirectMessenging() {
        final int selectedId = userListView.getSelectionModel().getSelectedIndex();
        if (selectedId == -1) {
            return;
        }
        HBox hBox = (HBox) userListView.getItems().get(selectedId);
        Label label = (Label) hBox.getChildren().get(1);
        switchToDirectMessage(label.getText());
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
        VBox currentMessageVBox = (VBox) messagesListView.getSelectionModel().getSelectedItem();
        HBox currentMessageHBox = (HBox) currentMessageVBox.getChildren().get(2);
        VBox currentMessageVBox2 = (VBox) currentMessageHBox.getChildren().get(1);
        Label currentMessageLabel = (Label) currentMessageVBox2.getChildren().get(1);

        // Our clipboard!
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        content.putString(currentMessageLabel.getText());

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
     * Closes the program
     */
    public void closeProgram() {
        // Closing the websocket by sending message we're on out way to the server!
        JsonObject submitMessage = wsClient.createSubmitObject(
                "logout",
                null,
                "",
                clientInfo.getUsername(),
                null
        );
        wsClient.send(submitMessage.toString().trim());
        Platform.exit();
        System.exit(0);
    }

    public void setClose(Stage stage) {
        // This will call the closeProgram() function in MainController so it closes correctly when
        // clicking on the red X!
        stage.setOnHidden(e -> closeProgram());
    }

    /**
     * This will get the user of the last message. This only works because the way we have structured our messages.
     */
    private String getPreviousUser(JsonArray newMessages, int i) {
        // We need to get the last message. To do that there are multiple ways. When changing rooms or logging in
        // the first time you'll get all the messages, and messagesListView will be empty. So we'll need to scan
        // there to get it.
        // Next, if messagesListView is filled, we'll grab the last of that.
        // Lets get the last message in messagesListView

        JsonObject lastMessage;
        String lastUser;

        if (messagesListView.getItems().size() > 0) {
            VBox localObject = (VBox) messagesListView.getItems().get(messagesListView.getItems().size() - 1);
            //HBox localHBox = (HBox) localObject.getChildren().get(0);
            Label localLabel = (Label) localObject.getChildren().get(0);
            lastUser = localLabel.getText();
        } else {
            // Now, we want the previous JsonObject in our iteration, else grab this one.
            if (0 <= i && i < newMessages.size() -1) {
                lastMessage = newMessages.get(i+1).getAsJsonObject();
                lastUser = lastMessage.get("username").toString().replace("\"", "");
            } else {
                lastMessage = newMessages.get(i).getAsJsonObject();
                lastUser = lastMessage.get("username").toString().replace("\"", "");
                lastUser = "";
            }
        }
        return lastUser;
    }


    // +++++++++++++++++++++++ UPDATER FUNCTIONS ++++++++++++++++++++++++++
    /**
     * To send a message to the console or the GUI
     */
    public void updateMessages(JsonArray newMessages) {
        if (newMessages == null) {
            return;
        }
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (int i = newMessages.size() -1; i >= 0; i--) {
            Message message = new Message((JsonObject) newMessages.get(i));
            messages.add(message);

            messageBoxes.add(
                    createMessageBox(
                            message.getUsername(),
                            message.getTimestamp(),
                            message.getMessage(),
                            message.getMessageId(),
                            false)
            );

            // Takes the messageId from the server and assigns it to the global messageIds
            // we'll use for the delete function.
            messageIds.add(message.getMessageId());
        }

        Platform.runLater(() -> {
            messagesListView.getItems().addAll(messageBoxes);
            // If we want to group our messages, we'll do it now.
            if (PreferencesObject.get("groupMessages").getAsBoolean()) {
                groupMessages();
            }
            //trimMessagesToMessageLimit();
            scrollToBottom();
        });

    }

    /**
     * To send a message to the console or the GUI
     */
    public void resetMessages() {
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (Message message: messages){
            messageBoxes.add(
                    createMessageBox(
                            message.getUsername(),
                            message.getTimestamp(),
                            message.getMessage(),
                            message.getMessageId(),
                            false)
            );

            // Takes the messageId from the server and assigns it to the global messageIds
            // we'll use for the delete function.
        }

        Platform.runLater(() -> {
            messagesListView.getItems().clear();
            messagesListView.getItems().addAll(messageBoxes);
            // If we want to group our messages, we'll do it now.
            if (PreferencesObject.get("groupMessages").getAsBoolean()) {
                groupMessages();
            }
            //trimMessagesToMessageLimit();
            scrollToBottom();
        });

    }

    // Now, if we have groupMessages true, we'll go through and combine them.
    public void groupMessages() {
        int start = -1;
        int end = -1;
        String lastUser = "";
        messageIds.clear();
        groupIndexes.clear();
        groupedMessageBoxes.clear();
        ArrayList<String> messagesToGroup = new ArrayList<>();
        for (int i=0; i < messagesListView.getItems().size(); i++) {
            String currentUser = "";
            VBox currentMessageVBox = (VBox) messagesListView.getItems().get(i);
            Label currentUserLabel = (Label) currentMessageVBox.getChildren().get(0);
            currentUser = currentUserLabel.getText();
            boolean sameUser = currentUser.equals(lastUser);

            if (sameUser && (start == -1)) {
                start = i-1;
            } else if ((!sameUser && (start != -1))) {
                end = i-1;
                groupIndexes.add(new int[]{start,end});
                start = -1;
                end = -1;
            }
            if (sameUser && (i == messagesListView.getItems().size() -1)) {
                end = i;
                groupIndexes.add(new int[]{start,end});
            }
            lastUser = currentUser;
        }

        // Now we'll go back and remove the sameUser messages and create one big one for each and reinsert them.
        for (int[] groupIndex : groupIndexes) {
            int indexStart = groupIndex[0];
            int indexEnd = groupIndex[1];
            String user = "";
            String timeStamp = "";
            String message = "";
            int currentMessageId = 0;
            Label currentTimeStampLabel = new Label();
            messagesToGroup.clear();

            for (int j = indexStart; j <= indexEnd; j++) {
                VBox currentMessageVBox = (VBox) messagesListView.getItems().get(j);
                Label currentUserLabel = (Label) currentMessageVBox.getChildren().get(0);
                Label currentMessageIdLabel = (Label) currentMessageVBox.getChildren().get(1);
                HBox currentMessageHBox = (HBox) currentMessageVBox.getChildren().get(2);
                VBox currentMessageVBox2 = (VBox) currentMessageHBox.getChildren().get(1);
                Label currentMessageLabel = (Label) currentMessageVBox2.getChildren().get(1);
                HBox currentTimeStampHBox = (HBox) currentMessageVBox2.getChildren().get(0);
                try {
                    currentTimeStampLabel = (Label) currentTimeStampHBox.getChildren().get(1);
                } catch (Exception e) {
                    currentTimeStampLabel = (Label) currentTimeStampHBox.getChildren().get(0);
                }
                timeStamp = currentTimeStampLabel.getText();
                message = currentMessageLabel.getText();
                //System.out.println("Adding Message: " + message);
                messagesToGroup.add(message);
                if (j == indexStart) {
                    user = currentUserLabel.getText();
                    currentMessageId = Integer.parseInt(currentMessageIdLabel.getText());
                }
            }
            StringBuilder finalMessage = new StringBuilder();
            for ( String individualMessage : messagesToGroup) {
                // Removes extra whitespace/newlines.
                individualMessage = individualMessage.trim();
                finalMessage.append(individualMessage).append("\n");
            }

            // Now, lets create a new message box and insert it back in!
            groupedMessageBoxes.add(
                    createMessageBox(
                            user,
                            timeStamp,
                            finalMessage.toString(),
                            currentMessageId,
                            true)
            );
        }

        Collections.reverse(groupIndexes);
        if (groupIndexes.size() > 0) {
            Platform.runLater(() -> {
                for (int[] groupIndex : groupIndexes) {
                    int indexStart = groupIndex[0];
                    int indexEnd = groupIndex[1];
                    messagesListView.getItems().remove(indexStart, indexEnd+1);
                    messagesListView.getItems().add(indexStart, groupedMessageBoxes.get(groupedMessageBoxes.size() - 1));
                    groupedMessageBoxes.remove(groupedMessageBoxes.size()-1);
                }
                trimMessagesToMessageLimit();
                scrollToBottom();
            });
        }
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
                        if (!label.getText().equals(clientInfo.getCurrentRoom())) {
                            switchRoom(label.getText());
                        } else {
                            logger.info("Not switching room since you chose the same room");
                        }
                    }
                }
            });

            roomListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        Integer roomIndex = roomListView.getFocusModel().focusedIndexProperty().getValue();
                        //logger.info("Room Index: " + roomIndex);
                        if (!roomLabels.get(roomIndex).getText().equals(clientInfo.getCurrentRoom())) {
                            switchRoom(roomLabels.get(roomIndex).getText());
                        } else {
                            logger.info("Not switching rooms, you chose the same room.");
                        }
                    }
                }
            });

            label.setText(trimUsers(obj.get("name").toString()));

            // Set room label font size
            label.setFont(new Font("Arial", PreferencesObject.get("fontSize").getAsInt() - 4));

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
        // Clear all users, we'll build it again further down.
        allUsers.clear();
        ArrayList<HBox> userHBox = new ArrayList<>();
        for (JsonElement user : users) {
            JsonObject userObject = new JsonObject();
            if (user.isJsonObject()) {
                userObject = user.getAsJsonObject();
            }
            HBox hBox = new HBox();
            Circle circle = new Circle();
            Label label = new Label();
            circle.setRadius(7);

            // If the user is active, set Circle to GREEN, RED otherwise.
            if (userObject.get("Status").getAsBoolean()) {
                circle.setFill(Color.LAWNGREEN);
            } else if (!userObject.get("Status").getAsBoolean()) {
                circle.setFill(Color.INDIANRED);
            }

            label.getStyleClass().add("font-color");
            label.setAlignment(Pos.CENTER);
            label.setText(trimUsers(userObject.get("username").toString()));
            // Adds the user to the user list!
            allUsers.add(trimUsers(userObject.get("username").toString()));
            label.setStyle("-fx-padding: 0 0 0 10;");

            // We want to change the logged in user to a special color.
            if (trimUsers(userObject.get("username").toString()).equals(clientInfo.getUsername())) {
                label.setTextFill(Color.ROYALBLUE);
            } else {
                label.setId("userLabelID");
            }

            // Set user label font size
            label.setFont(new Font(PreferencesObject.get("fontSize").getAsInt() - 4));

            hBox.getChildren().addAll(circle, label);
            userHBox.add(hBox);

            // In case you select the label within the list
            label.setOnMouseClicked(new EventHandler<MouseEvent>(){
                @Override
                public void handle(MouseEvent event){
                    if (event.getClickCount() == 2) {
                        if (label.getText() != clientInfo.getCurrentRoom()) {
                            switchToDirectMessage(label.getText());
                            clientInfo.setCurrentRoom(label.getText());
                        } else {
                            logger.info("Not switching room since you chose the same room");
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
                    Label label = new Label();
                    // The second element, that is the one that has the label.
                    label = (Label) userHBox.get(userIndex).getChildren().get(1);
                    switchToDirectMessage(label.getText());
                    clientInfo.setCurrentRoom(label.getText());
                }
            }
        });

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userListView.getItems().clear();
                userListView.getItems().addAll(userHBox);
            }
        });
    }

    // +++++++++++++++++++++++ CONNECTION FUNCTIONS ++++++++++++++++++++++++++
    public void setConnectionInformation() {
        // If latency is less than 500ms, circle will be green.
        if (wsClient.latency <= 500) {
            latencyCircle.setFill(Color.LAWNGREEN);
        } else if (wsClient.latency > 500 && wsClient.latency <= 1000) {
            latencyCircle.setFill(Color.YELLOW);
        } else if (wsClient.latency > 1000) {
            latencyCircle.setFill(Color.INDIANRED);
        }
        connectionInformation.setText("Remote Server: " + wsClient.getConnection().getRemoteSocketAddress() + " - Latency: " + wsClient.getLatency() + "ms");
    }

    /**
     * Sets the stage for the connection information bar
     */
    public void setupConnectionInformation() {
        connectionInformation.setVisible(false);
        connectionInformation.setManaged(false);
        latencyCircle.setVisible(false);
        latencyCircle.setManaged(false);
    }

    /**
     * Toggle displaying the Connection Info banner
     */
    @FXML
    public void toggleConnInfo() {
        if (connectionInformation.isVisible()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    connectionInformation.setVisible(false);
                    connectionInformation.setManaged(false);
                    latencyCircle.setVisible(false);
                    latencyCircle.setManaged(false);
                }
            });
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    wsClient.sendAPing();
                    connectionInformation.setVisible(true);
                    connectionInformation.setManaged(true);
                    latencyCircle.setVisible(true);
                    latencyCircle.setManaged(true);
                }
            });
        }
    }

    // +++++++++++++++++++++++ HELPER FUNCTIONS ++++++++++++++++++++++++++
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


    /**
     * Creates a message box with proper formatting
     */
    private VBox createMessageBox(String username, String timestamp, String message, Integer messageId, Boolean sameUser) {
        // We're doing this because if you toggle between grouping messages and not, you'll be passing in a timestamp
        // that is formatted differently.
        String shortTime = "";
        try {
            shortTime = convertTime(timestamp.replace("\"", ""));
        } catch (Exception e) {
            shortTime = timestamp;
        }

        //Loading image from imageMap
        ImageView imv = new ImageView();
        int imageWidth = 40;
        int imageHeight = 40;
        imv.setFitWidth(imageWidth);
        imv.setFitHeight(imageHeight);
        try {
            Image image = imageMap.get(username);
            imv.setImage(image);
        } catch (Exception e) {
            Image image = new Image(MainController.class.getResourceAsStream(
                    "../images/profilePics/default.jpg"),
                    imageWidth, imageHeight, false, false);
            imv.setImage(image);
        }

        imv.setId("profilePic");


        Label hiddenUsername = new Label();
        Label hiddenMessageId = new Label();
        VBox individualMessageVBox = new VBox();
        VBox individualMessageContainer = new VBox();
        HBox hBox = new HBox();
        HBox hBox1 = new HBox();

        // Not sure what this does. Commenting out for now.
        // individualMessageVBox.setStyle(".messageBox");
        individualMessageContainer.setMaxWidth(Region.USE_PREF_SIZE);
        Label usernameLabel = new Label();
        Label timeStampLabel = new Label();
        Label messageLabel = new Label();

        usernameLabel.getStyleClass().add("font-color");
        timeStampLabel.getStyleClass().add("font-color");
        messageLabel.getStyleClass().add("font-color");

        usernameLabel.setText(username);
        timeStampLabel.setText(shortTime);
        messageLabel.setText(message);

        // Here we hide the user if the previous message if from the same user.
        // Furthermore, we will always set the user to a hidden label so we can grab it.
        // Otherwise, when we try and grab a message that doesn't have a user label it won't work.
        if (sameUser && PreferencesObject.get("groupMessages").getAsBoolean()) {
            hBox.getChildren().addAll(timeStampLabel);
        } else {
            hBox.getChildren().addAll(usernameLabel, timeStampLabel);
        }

        hiddenUsername.setText(username);
        hiddenUsername.setVisible(false);
        hiddenUsername.setManaged(false);
        hiddenMessageId.setText(messageId.toString());
        hiddenMessageId.setVisible(false);
        hiddenMessageId.setManaged(false);

        individualMessageContainer.getChildren().addAll(hBox, messageLabel);

        // check if username == the current user or moves messages to the right
        if ((!username.equals(clientInfo.getUsername())) && (!username.equals("admin"))) {
            hBox1.setAlignment(Pos.CENTER_RIGHT);
            hBox.setAlignment(Pos.CENTER_RIGHT);
            individualMessageVBox.getStyleClass().add("otherMessageBox");
            individualMessageContainer.setId("otherMessageBox");
            hBox1.getChildren().add(individualMessageContainer);
            hBox1.getChildren().add(imv);
        } else if (username.equals("admin")){
            // must be admin, we want to center these messages
            hBox1.setAlignment(Pos.CENTER);
            hBox.setAlignment(Pos.CENTER);
            individualMessageVBox.getStyleClass().add("adminMessageBox");
            individualMessageContainer.setId("adminMessageBox");
        } else {
            // Must be the user!
            hBox1.setAlignment(Pos.CENTER_LEFT);
            hBox.setAlignment(Pos.CENTER_LEFT);
            individualMessageVBox.getStyleClass().add("userMessageBox");
            individualMessageContainer.setId("userMessageBox");
            hBox1.getChildren().add(imv);
            hBox1.getChildren().add(individualMessageContainer);
        }

        individualMessageVBox.getChildren().add(hiddenUsername);
        individualMessageVBox.getChildren().add(hiddenMessageId);
        //individualMessageVBox.getChildren().add(imv);
        individualMessageVBox.getChildren().add(hBox1);

        // Set timestamp font size
        timeStampLabel.setFont(new Font("Arial", PreferencesObject.get("fontSize").getAsInt() - 4));
        timeStampLabel.setPadding(new Insets(0, 0, 0, 10));

        // Set Message font size
        messageLabel.setFont(new Font("Arial", PreferencesObject.get("fontSize").getAsInt()));

        return individualMessageVBox;
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
        String line2 = ("Designed and built by Brody and Maxwell in Utah!" + "\n" + "https://github.com/MaxwellM/Orbit-Messenger" + "\n\n" + "Version: " + version);
        // create a hyperlink
        Hyperlink hyperlink = new Hyperlink("hyperlink");
        alert.setContentText(line2);
        alert.showAndWait();
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
     * Deletes a message locally by messageId sent from the server.
     *
     * @param messageId
     */
    private void deleteMessages(Integer messageId) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                int index = messageIds.indexOf(messageId);
                messagesListView.getItems().remove(index);
                messageIds.remove(index);
            }
        });
    }

    // +++++++++++++ Links to other views ++++++++++++++++
    /**
     * Opens the create room window
     */
    public void openCreateRoom() {
        logger.info("Opening Create Room!");
        CreateRoomController createRoom = new CreateRoomController();
        createRoom.setServer(clientInfo.getHttpServer());
        Stage newStage = new Stage();
        newStage.setTitle("Create Room");
        createRoom.changeSceneTo(this.CROOM_FXML, createRoom, newStage);

        Thread createRoomThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (getAllShowingStages(newStage.getTitle()).size() > 0) {
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
        logger.info("Opening Preferences!");
        PreferencesController pref = new PreferencesController(clientInfo.getHttpServer(), clientInfo.getUsername(), clientInfo.getPassword());
        Stage newStage = new Stage();
        newStage.setTitle("Preferences");
        pref.changeSceneTo(this.PREF_FXML, pref, newStage);

        Thread updatePreferences = new Thread(new Runnable() {
            @Override
            public void run() {
                while (getAllShowingStages(newStage.getTitle()).size() > 0) {
                    try {
                        Thread.sleep(500); // Milliseconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //getAllImages();
                clearMessages();
                loadPreferences();
                sendProperties();
                setDarkMode();
            }
        });
        updatePreferences.start();
    }

    private void clearMessages() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                messagesListView.getItems().clear();
                }
        });
    }

    private ObservableList<Stage> getAllShowingStages(String name) {
        ObservableList<Stage> stages = FXCollections.observableArrayList();
        Window.getWindows().forEach(w -> {
            if ((w instanceof Stage) && (name.equals(((Stage) w).getTitle()))){
                stages.add((Stage) w);
            }
        });
        return stages;
    }

    /**
     * Queries the server, sending each user, obtaining their profile picture.
     */
    public void getAllImages() {
        // Clears the imageMap so we can fill it up again.
        imageMap.clear();
        for (String user : allUsers) {
            try {
                // This was we can get an image as a byte array. Then, keeping it in memory, we can convert it to
                // an image. Finally, assign that image to our imageMap. Voila!
                byte[] fileBytes = Unirest.get(this.clientInfo.getHttpServer() + "/getAvatar")
                        .basicAuth(clientInfo.getUsername(), clientInfo.getPassword())
                        .queryString("username", user)
                        .asBytes()
                        .getBody();
                ByteArrayInputStream input_stream= new ByteArrayInputStream(fileBytes);
                BufferedImage final_buffered_image = ImageIO.read(input_stream);
                Image image = SwingFXUtils.toFXImage(final_buffered_image, null);
                imageMap.put(user, image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                        clientInfo.getUsername(),
                        null
                );
                try {
                    wsClient.send(submitMessage.toString().trim());
                } catch (Exception e){
                    logger.warning("Error sending logout message to the sever: " + e.toString());
                }

                // Need to stop the running threads!
                //pingThread.stop();
                wsConnectionThread.stop();
                connectionInformationThread.stop();
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
}
