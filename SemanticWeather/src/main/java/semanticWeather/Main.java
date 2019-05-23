package semanticWeather;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private Controller controller;
    private ModelManager modelManager;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/guigui.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader();
        Controller controller = fxmlLoader.getController();

        primaryStage.setTitle("Amazing WeatherData Browser");
        primaryStage.setScene(new Scene(root, 980, 720));
        primaryStage.show();

        modelManager = ModelManager.getInstance();
    }

    public Controller getController(){
        return controller;
    }


    public static void main(String[] args) {
        launch(args);
    }
}
