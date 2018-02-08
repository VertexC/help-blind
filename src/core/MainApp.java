package core;

import core.view.RootLayoutController;
import core.view.VideoPlayerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application{
    private Stage primaryStage;
    private BorderPane rootLayout;
    private String openedFilePath;
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("BlindHelper");
        this.openedFilePath = null;
        initRootLayout();
        showVideoPlayer();
    }

    public void initRootLayout(){
        try {
            // load in FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            // give the controller access to the main app
            RootLayoutController controller = loader.getController();
            controller.setMainApp(this);

            // show the Scene
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void showVideoPlayer(){
        try {
            // load in FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/VideoPlayer.fxml"));
            AnchorPane videoPlayer  = (AnchorPane) loader.load();

            // give the controller access to the main app
            VideoPlayerController controller = loader.getController();
            controller.setMainApp(this);

            // set VideoPlayer
            rootLayout.setCenter(videoPlayer);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void setOpenedFilePath(String path){
        this.openedFilePath = path;
    }

    public String getOpenedFilePath(){
        return this.openedFilePath;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args){
        launch(args);
    }
}
