package dk.easv;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ImageViewerWindowController implements Initializable {
    private final List<Image> images = new ArrayList<>();
    public ButtonBar bBar;
    public Text text;
    private int currentImageIndex = 0;
    AtomicReference<ScheduledExecutorService> scheduledExecutorService = new AtomicReference<>();
    BooleanProperty isActive = new SimpleBooleanProperty(false);


    @FXML
    Parent root;

    @FXML
    private ImageView imageView;

    @FXML
    private void handleBtnLoadAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select image files");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Images",
                "*.png", "*.jpg", "*.gif", "*.tif", "*.bmp"));
        List<File> files = fileChooser.showOpenMultipleDialog(new Stage());

        if (!files.isEmpty()) {
            files.forEach((File f) ->
            {
                images.add(new Image(f.toURI().toString()));
            });
            displayImage();
        }
    }

    @FXML
    private void handleBtnPreviousAction() {
        if (!images.isEmpty()) {
            currentImageIndex =
                    (currentImageIndex - 1 + images.size()) % images.size();
            displayImage();
        }
    }

    @FXML
    private void handleBtnNextAction() {
        if (!images.isEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % images.size();
            displayImage();
        }
    }

    private void displayImage() {
        if (!images.isEmpty()) {
            imageView.setImage(images.get(currentImageIndex));
            File file = new File(images.get(currentImageIndex).getUrl());
            text.setText(file.getName().split("\\.")[0]);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Thread changeImage = new Thread(this::handleBtnNextAction);
        DoubleProperty time = new SimpleDoubleProperty(1);
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        Slider slider = new Slider();
        bBar.getButtons().add(startButton);
        bBar.getButtons().add(stopButton);
        bBar.getButtons().add(slider);

        startButton.setOnAction((s) -> {
            isActive.set(true);
            startSlideshow(changeImage, time);
        });
        slider.setOnMouseReleased(v -> {
            time.set(1 + slider.getValue() * 5 / 100);
            if (isActive.get()) {
                startSlideshow(changeImage,time);
            }
        });

        stopButton.setOnAction((s) -> {
            scheduledExecutorService.get().shutdownNow();
            isActive.set(false);
        });

    }

    public void closeThreadIfActive(){
        if(isActive.get())
            scheduledExecutorService.get().shutdownNow();
    }

    private void startSlideshow(Thread changeImage, DoubleProperty time) {
        if (scheduledExecutorService.get()!=null&&!scheduledExecutorService.get().isShutdown()){
            scheduledExecutorService.get().shutdownNow();
        }
        scheduledExecutorService.set(Executors.newSingleThreadScheduledExecutor());
        scheduledExecutorService.get().scheduleAtFixedRate(changeImage, time.intValue(), time.intValue(), TimeUnit.SECONDS);
    }
}