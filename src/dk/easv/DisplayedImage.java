package dk.easv;

import javafx.scene.image.Image;

public class DisplayedImage {
    boolean isDisplayed = false;
    Image image;

    DisplayedImage(Image image){
        this.image=image;
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    public void setDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }

    public Image getImage() {
        return image;
    }
}
