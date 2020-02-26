package com.orbitmessenger.utils;

import com.google.gson.JsonObject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.HashMap;

public class MessageBox extends CustomControls{
    private ImageView imageView;
    private Label hiddenUsername;
    private  Label hiddenMessageId;
    private VBox individualMessageVBox;
    private VBox individualMessageContainer;
    private HBox hBox;
    private HBox hBox1;
    private Label usernameLabel;
    private Label timeStampLabel;
    private Label messageLabel;


    private void createMessageBoxStructure(int fontsize){
        this.hiddenUsername = new Label();
        this.hiddenMessageId = new Label();
        this.individualMessageVBox = new VBox();
        this.individualMessageContainer = new VBox();
        this.hBox = new HBox();
        this.hBox1 = new HBox();

        // Not sure what this does. Commenting out for now.
        // individualMessageVBox.setStyle(".messageBox");
        individualMessageContainer.setMaxWidth(Region.USE_PREF_SIZE);
        this.usernameLabel = new Label();
        this.timeStampLabel = new Label();
        this.messageLabel = new Label();

        usernameLabel.getStyleClass().add("font-color");
        timeStampLabel.getStyleClass().add("font-color");
        messageLabel.getStyleClass().add("font-color");
        hiddenUsername.setVisible(false);
        hiddenUsername.setManaged(false);
        hiddenMessageId.setVisible(false);
        hiddenMessageId.setManaged(false);
        // Set timestamp font size
        timeStampLabel.setFont(new Font("Arial", fontsize - 4));
        timeStampLabel.setPadding(new Insets(0, 0, 0, 10));

        // Set Message font size
        messageLabel.setFont(new Font("Arial", fontsize));
    }
    public MessageBox(){}

    public VBox createMessageBox(String username, String timestamp,
                                 String message, Integer messageId, Boolean sameUser,
                                 HashMap<String, Image> imageMap, JsonObject preferencesObject, String currentUser){
        // We're doing this because if you toggle between grouping messages and not, you'll be passing in a timestamp
        // that is formatted differently.
        String shortTime = this.convertTime(timestamp.replace("\"", ""));
        this.imageView = createImageView(username, imageMap);
        this.createMessageBoxStructure(preferencesObject.get("fontSize").getAsInt());

        usernameLabel.setText(username);
        timeStampLabel.setText(shortTime);
        messageLabel.setText(message);

        // Here we hide the user if the previous message if from the same user.
        // Furthermore, we will always set the user to a hidden label so we can grab it.
        // Otherwise, when we try and grab a message that doesn't have a user label it won't work.
        if (sameUser && preferencesObject.get("groupMessages").getAsBoolean()) {
            hBox.getChildren().addAll(timeStampLabel);
        } else {
            hBox.getChildren().addAll(usernameLabel, timeStampLabel);
        }

        hiddenUsername.setText(username);
        hiddenMessageId.setText(messageId.toString());
        individualMessageContainer.getChildren().addAll(hBox, messageLabel);

        // check if username == the current user or moves messages to the right
        Pos position;
        String cssStyle;
        if ((!username.equals(currentUser))) {
            position = Pos.CENTER_RIGHT;
            cssStyle = "otherMessageBox";
        } else {
            // Must be the user!
            position = Pos.CENTER_LEFT;
            cssStyle = "userMessageBox";
        }
        hBox1.setAlignment(position);
        hBox.setAlignment(position);
        individualMessageVBox.getStyleClass().add(cssStyle);
        individualMessageContainer.setId(cssStyle);
        hBox1.getChildren().add(this.imageView);
        hBox1.getChildren().add(individualMessageContainer);

        individualMessageVBox.getChildren().add(hiddenUsername);
        individualMessageVBox.getChildren().add(hiddenMessageId);
        //individualMessageVBox.getChildren().add(imv);
        individualMessageVBox.getChildren().add(hBox1);
        return individualMessageVBox;
    }
}
