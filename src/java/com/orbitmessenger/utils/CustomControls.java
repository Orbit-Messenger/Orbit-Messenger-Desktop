
package com.orbitmessenger.utils;

import com.orbitmessenger.Controllers.MainController;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

class CustomControls {
    protected String convertTime(String time) {
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

    public ImageView createImageView(String username, HashMap<String, Image> imageMap) {
        int imgHeightWidth = 40;
        ImageView imv = new ImageView();
        imv.setFitWidth(imgHeightWidth);
        imv.setFitHeight(imgHeightWidth);
        try {
            Image image = imageMap.get(username);
            imv.setImage(image);
        } catch (Exception e) {
            Image image = new Image(MainController.class.getResourceAsStream(
                    "../images/profilePics/default.jpg"),
                    imgHeightWidth, imgHeightWidth, false, false);
            imv.setImage(image);
        }

        imv.setId("profilePic");
        return imv;
    }
}
