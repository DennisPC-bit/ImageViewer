package dk.easv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ImageViewerWindow.fxml"));
        Parent root = loader.load();
        ImageViewerWindowController imageViewerWindowController = loader.getController();
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Image Viewer");
        primaryStage.show();
        primaryStage.setOnCloseRequest(v -> imageViewerWindowController.closeThreadIfActive());
    }


    public static void main(String[] args) {
        launch(args);
    }
}
