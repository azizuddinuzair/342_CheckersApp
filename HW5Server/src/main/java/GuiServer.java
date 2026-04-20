import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application {

	private Server serverConnection;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		serverConnection = new Server(data -> System.out.println(String.valueOf(data)));

		StackPane pane = new StackPane();
		pane.setAlignment(Pos.CENTER);
		pane.getChildren().add(new Label("Server running"));

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(new Scene(pane, 250, 120));
		primaryStage.setTitle("Server");
		primaryStage.show();
	}
}