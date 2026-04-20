import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application {

	private Server serverConnection;
	private ListView<String> logList;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		logList = new ListView<String>();

		serverConnection = new Server(data -> {
			String logLine = String.valueOf(data);
			System.out.println(logLine);
			Platform.runLater(() -> logList.getItems().add(logLine));
		});

		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(10));
		pane.setCenter(logList);

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(new Scene(pane, 700, 500));
		primaryStage.setTitle("Server Logs");
		primaryStage.show();
	}
}
