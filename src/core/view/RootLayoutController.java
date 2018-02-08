package core.view;

import com.sun.tools.javac.Main;
import core.MainApp;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RootLayoutController {

    private Stage rootStage;
    private MainApp mainApp;

    public void setRootStage(Stage stage){
        this.rootStage = stage;
    }

    public void setMainApp(MainApp mainApp){
        this.mainApp = mainApp;
    }
    @FXML
    private void handleImport(){
        Stage dialogStage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open the Source File");
        fileChooser.showOpenDialog(dialogStage);
    }
}
