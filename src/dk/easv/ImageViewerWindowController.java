package dk.easv;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;


public class ImageViewerWindowController implements Initializable {
    @FXML
    public HBox hBox;
    @FXML
    private ButtonBar bBar;
    @FXML
    private Text text;
    @FXML
    private Slider slider;
    @FXML
    private BorderPane root;
    @FXML
    private ImageView imageView;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button multiview;
    @FXML
    private Button maximize;
    @FXML
    private Button close;
    @FXML
    private Button closeAll;
    @FXML
    private HBox colorDisplay = new HBox();

    private static boolean menuBarIsVisible = true;
    private int currentImageIndex = 0;
    private static int i = 0;
    private static final DoubleProperty time = new SimpleDoubleProperty(1);
    private static final AtomicReference<ScheduledExecutorService> scheduledExecutorService = new AtomicReference<>();
    private final BooleanProperty isActive = new SimpleBooleanProperty(false);
    private Main main;


    private static final List<DisplayedImage> images = new ArrayList<>(Arrays.asList());
    protected static final List<ImageViewerWindowController> cons = new ArrayList<>();
    protected static final List<Stage> stages = new ArrayList<>();
    private Stage stage;
    private final Thread changeImage = new Thread(() -> {
        cons.get(i % cons.size()).handleBtnNextAction();
        i++;
    });

    public ImageViewerWindowController() {
        cons.add(this);
    }

    public void setMain(Main main) {
        this.main = main;
    }

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
        cons.forEach(ImageViewerWindowController::handleBtnNextAction);
    }

    @FXML
    private void handleBtnPreviousAction() {

        int j = (int) images.stream().filter(i -> i.isDisplayed).count();
        if (!images.isEmpty() && j < images.size()) {
            while (images.get(currentImageIndex).isDisplayed) {
                currentImageIndex = (currentImageIndex - 1 + images.size()) % images.size();
            }
            displayImage();
        }
    }

    @FXML
    private void handleBtnNextAction() {
        int j = (int) images.stream().filter(i -> i.isDisplayed).count();
        if (!images.isEmpty() && j < images.size()) {
            while (images.get(currentImageIndex).isDisplayed) {
                currentImageIndex = (currentImageIndex + 1) % images.size();
            }
            displayImage();
        }
    }

    private void displayImage() {
        if (!images.isEmpty()) {
            images.forEach(i -> {
                if (i.getImage() == imageView.getImage())
                    i.setDisplayed(false);
            });
            images.get(currentImageIndex).setDisplayed(true);
            Image image = images.get(currentImageIndex).getImage();
            imageView.setImage(image);
            File file = new File(images.get(currentImageIndex).getImage().getUrl());
            text.setText(file.getName().split("\\.")[0]);

            go(image, 6);
        }
    }

    public void go(Image image, int threads) {
        Instant start = Instant.now();
        int width = (int) image.getWidth() / threads;
        List<ImageAnalyzer> anal = new ArrayList<>();
        List<int[]> bit = new ArrayList<>();
        int startIndex = 0;
        for (int i = 1; i <= threads; i++) {
            if (i < threads)
                bit.add(new int[]{startIndex + 1, width * i});
            else
                bit.add(new int[]{startIndex + 1, (int) image.getWidth()});
            startIndex = width * i;
        }
        bit.forEach(b -> {anal.add(new ImageAnalyzer(image, b[0], b[1], 0, (int) image.getHeight()));
        });
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try {
            List<Future<Map<String, Integer>>> results = executorService.invokeAll(anal);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Platform.runLater(new Thread(() -> {
            colorDisplay.getChildren().clear();
            List<String> colors = new ArrayList<>(ImageAnalyzer.getColorMap().keySet());
            colors.sort(Comparator.comparingInt(c -> ImageAnalyzer.getColorMap().get(c)).reversed());
            AtomicInteger j = new AtomicInteger(0);
            ImageAnalyzer.getColorMap().values().forEach(j::addAndGet);
            for (String color : colors) {
                Circle c = new Circle(8, Paint.valueOf(color));
                Text t = new Text(String.format("%s(%.02f%%)", color, (float) ImageAnalyzer.getColorMap().get(color) * 100 / j.get()));
                colorDisplay.setAlignment(Pos.CENTER);
                colorDisplay.setSpacing(10);
                colorDisplay.getChildren().addAll(Arrays.asList(c, t));
            }
            ImageAnalyzer.clearColorMap();
        }));
        executorService.shutdown();

        Instant end = Instant.now();
        System.out.println(Duration.between(start,end).toMillis() + " ms");
    }

    public boolean maximized = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        close.setStyle("-fx-background-color: red");

        maximize.setOnAction(v -> {
            main.getPrimaryStage().setFullScreen(!maximized);
            maximized = !maximized;
        });

        imageView.setFitHeight((double) root.heightProperty().getValue() - bBar.getHeight() - colorDisplay.getHeight() - 27);

        root.heightProperty().addListener((observable, t1, t2) -> {
            if (menuBarIsVisible)
                imageView.setFitHeight((double) t2 - bBar.getHeight() - colorDisplay.getHeight() - 27);
            else
                imageView.setFitHeight((double) t2);
        });
        root.widthProperty().addListener((observable, t1, t2) ->
                imageView.setFitWidth((double) t2));

        close.setOnAction(v -> {
            if (main != null)
                main.closeMainStage();
            else
                stage.hide();
        });
        closeAll.setOnAction(v -> stages.forEach(Stage::hide));

        multiview.setOnAction(this::openNewStage);

        startButton.setOnAction((s) -> startSlideshow(changeImage, time));

        slider.setOnMouseReleased(v -> setSlideshowSpeed(changeImage, slider));

        stopButton.setOnAction(v -> stopSlideshow());

        moveMainView();

    }

    public void setMenuBarVisible() {
        if (menuBarIsVisible) {
            cons.forEach(c -> {
                c.root.setTop(null);
                c.root.setBottom(null);
            });
        } else {
            cons.forEach(c -> {
                c.root.setTop(c.hBox);
                c.root.setBottom(colorDisplay);
            });
        }
        menuBarIsVisible = !menuBarIsVisible;
    }

    public Slider getSlider() {
        return slider;
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
        scheduledExecutorService.get().scheduleAtFixedRate(changeImage, 0, time.intValue(), TimeUnit.SECONDS);
        isActive.set(true);
    }

    private void stopSlideshow() {
        if (scheduledExecutorService.get() != null)
            scheduledExecutorService.get().shutdownNow();
        isActive.set(false);
    }

    private void setSlideshowSpeed(Thread thread, Slider slider) {
        time.set(1 + slider.getValue() * 5 / 100);
        if (isActive.get()) {
            startSlideshow(thread, time);
        }
        cons.forEach(c -> c.getSlider().setValue(slider.getValue()));
    }

    private void openNewStage(ActionEvent v) {
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ImageViewerWindow.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageViewerWindowController imageViewerWindowController = loader.getController();
        Scene scene = new Scene(root);
        stages.add(stage);
        imageViewerWindowController.setStage(stage);
        stage.setScene(scene);
        stage.setTitle("Image Viewer");
        stage.show();
        stage.setOnCloseRequest(d -> imageViewerWindowController.closeThreadIfActive());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Moves the main view when pulling the top bar
     */
    public void moveMainView() {
        AtomicReference<Double> x = new AtomicReference<>((double) 0);
        AtomicReference<Double> y = new AtomicReference<>((double) 0);
        bBar.setOnMousePressed(mouseEvent -> {
            x.set(mouseEvent.getSceneX());
            y.set(mouseEvent.getSceneY());
        });
        bBar.setOnMouseDragged(mouseEvent -> {
                    if (main != null) {
                        main.getPrimaryStage().setX(mouseEvent.getScreenX() - x.get());
                        main.getPrimaryStage().setY(mouseEvent.getScreenY() - y.get());
                    } else {
                        stage.setX(mouseEvent.getScreenX() - x.get());
                        stage.setY(mouseEvent.getScreenY() - y.get());
                    }
                }

        );
    }
}