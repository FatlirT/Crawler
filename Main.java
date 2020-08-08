import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    // Set up GUI.
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("crawler.fxml"));
        primaryStage.setTitle("Crawler");
        primaryStage.getIcons().add(new Image(String.valueOf(getClass().getResource("Crawler.png"))));
        primaryStage.setScene(new Scene(root, 700, 275));
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(275);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // Launch GUI.
    public static void main(String[] args) {
        launch(args);
    }
}
