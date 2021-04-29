package dk.easv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ImageViewerWindow.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 800,600);
        this.primaryStage=primaryStage;
        ImageViewerWindowController imageViewerWindowController = loader.getController();
        imageViewerWindowController.setMain(this);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Image Viewer");
        primaryStage.show();
        primaryStage.setOnCloseRequest(v -> imageViewerWindowController.closeThreadIfActive());
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public  void closeMainStage(){
        primaryStage.hide();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
