package dk.easv;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.xml.transform.Result;

public class ImageViewerWindowController implements Initializable {
    static int i = 0;
    private static final List<DisplayedImage> images = new ArrayList<>();
    public ButtonBar bBar;
    public Text text;
    private int currentImageIndex = 0;
    List<ImageViewerWindowController> cons = new ArrayList<>(Arrays.asList(this));
    Thread changeImage = new Thread(()->{
        cons.get(i%cons.size()).handleBtnNextAction();
        i++;
    });
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
            files.forEach((File f) -> images.add(new DisplayedImage(new Image(f.toURI().toString()))));
            displayImage();
        }
    }

    @FXML
    private void handleBtnPreviousAction() {
        if (!images.isEmpty()) {
            while (images.get(currentImageIndex).isDisplayed) {
                currentImageIndex = (currentImageIndex - 1 + images.size()) % images.size();
            }
            displayImage();
        }
    }

    @FXML
    private void handleBtnNextAction() {
        if (!images.isEmpty()) {
            while (images.get(currentImageIndex).isDisplayed) {
                currentImageIndex = (currentImageIndex + 1) % images.size();
            }
            displayImage();
        }
    }

    private void displayImage() {
        if (!images.isEmpty()) {
            images.forEach(i->{
                if(i.getImage()==imageView.getImage())
                    i.setDisplayed(false);
            });
            images.get(currentImageIndex).setDisplayed(true);
            Image image = images.get(currentImageIndex).getImage();
            imageView.setImage(image);
            File file = new File(images.get(currentImageIndex).getImage().getUrl());
            text.setText(file.getName().split("\\.")[0]);
            /*
                go(image, 4);
             */
        }
    }

    private void go(Image image, int threads) {
        Instant start = Instant.now();
        List<ImageAnalyzer> anal = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int width = (int) image.getWidth() / threads;
            anal.add(new ImageAnalyzer(image, 1 + (i * width), (i + 1) * width, 1, (int) image.getHeight()));
        }
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try {
            executorService.invokeAll(anal);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        Instant end = Instant.now();
        double count = 0;
        for (Integer i : ImageAnalyzer.getColorIntegerHashMap().values())
            count += i;
        double finalCount = count;
        ImageAnalyzer.getColorIntegerHashMap().keySet().forEach(key -> System.out.printf("%s's Count: %s (%.02f%%) ", key, ImageAnalyzer.getColorIntegerHashMap().get(key), ImageAnalyzer.getColorIntegerHashMap().get(key) * 100 / finalCount));
        System.out.println();

        System.out.println("Duration: " + Duration.between(start, end).toMillis() + " ms" + "(Threads: " + threads + ")");
        ImageAnalyzer.colorIntegerHashMap.clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        DoubleProperty time = new SimpleDoubleProperty(1);
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        Button multiview = new Button("multiView");
        Slider slider = new Slider();
        bBar.getButtons().add(startButton);
        bBar.getButtons().add(stopButton);
        bBar.getButtons().add(slider);
        bBar.getButtons().add(multiview);

        multiview.setOnAction(v -> {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ImageViewerWindow.fxml"));
            Parent root = null;
            try {
                root = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ImageViewerWindowController imageViewerWindowController = loader.getController();
            cons.add(imageViewerWindowController);
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Image Viewer");
            stage.show();
            stage.setOnCloseRequest(d -> imageViewerWindowController.closeThreadIfActive());
        });

        startButton.setOnAction((s) -> {
            isActive.set(true);
            startSlideshow(changeImage, time);
        });
        slider.setOnMouseReleased(v -> {
            time.set(1 + slider.getValue() * 5 / 100);
            if (isActive.get()) {
                startSlideshow(changeImage, time);
            }
        });

        stopButton.setOnAction((s) -> {
            if(scheduledExecutorService!=null)
            scheduledExecutorService.get().shutdownNow();
            isActive.set(false);
        });

    }

    public Thread getThread() {
        return changeImage;
    }

    public void closeThreadIfActive() {
        if (isActive.get())
            scheduledExecutorService.get().shutdownNow();
    }

    private void startSlideshow(Thread changeImage, DoubleProperty time) {
        if (scheduledExecutorService.get() != null && !scheduledExecutorService.get().isShutdown()) {
            scheduledExecutorService.get().shutdownNow();
        }
        scheduledExecutorService.set(Executors.newSingleThreadScheduledExecutor());
        scheduledExecutorService.get().scheduleAtFixedRate(changeImage, time.intValue(), time.intValue(), TimeUnit.SECONDS);
    }
}