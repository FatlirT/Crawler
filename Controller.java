import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable{

    // The file path String.
    @FXML
    private TextField directory;
    // Initiates the logic.
    @FXML
    private Button goButton;
    // Select file.
    @FXML
    private Button selectFileButton;
    // Shows the state of the program.
    @FXML
    private Label statusLabel;
    // Main layout.
    @FXML
    private BorderPane borderPane;
    // Checked to overwrite current prices in file.
    @FXML
    private RadioButton overwriteButton;

    // Logic component.
    private Crawler crawler;

    // Initialise GUI and initialise logic.
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        crawler = new Crawler();
    }

    // Select a file. Called by 'Select File' button. Shows a file selection dialog.
    @FXML
    private void selectFile(){

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");

        FileChooser.ExtensionFilter xlsx = new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx, *.xls)", "*.xlsx", "*.xls");
        fileChooser.getExtensionFilters().add(xlsx);
        Stage stage = (Stage) borderPane.getScene().getWindow();

        File file = fileChooser.showOpenDialog(stage);

        if(file != null) {
            directory.setText(file.getAbsolutePath());
            statusLabel.setText("Ready");
            goButton.setDisable(false);
        }

    }

    // Called by 'Go' button. Initiates the logic processes. Disables 'Go' button so
    // as not to interfere with current process. Also sets status label to indicate this.
    @FXML
    private void go(){

        statusLabel.setText("Processing...");
        goButton.setDisable(true);
        crawl();

    }

    // Handles the results of the completed Logic processes and updates GUI accordingly.
    private void crawl(){

        try{
            crawler.updatePrices(directory.getText(), overwriteButton.isSelected());
            statusLabel.setText("Success");
            goButton.setDisable(false);
        } catch(IOException e){
            statusLabel.setText("Unable to process file: close file if open or check internet connection...");
            goButton.setDisable(false);
        }
    }

}