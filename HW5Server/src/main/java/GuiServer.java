
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application{

	HashMap<String, Scene> sceneMap;
	Server serverConnection;
	
	ListView<String> listItems;
	
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems = new ListView<String>();

		serverConnection = new Server(data -> {
			Platform.runLater(()->{
				listItems.getItems().add(ChatLogs(data));
			});
		});

		sceneMap = new HashMap<String, Scene>();
		
		sceneMap.put("server",  createServerGui());
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

		primaryStage.setScene(sceneMap.get("server"));
		primaryStage.setTitle("This is the Server");
		primaryStage.show();
		
	}
	
	public Scene createServerGui() {
		
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(70));
		pane.setStyle("-fx-background-color: coral");
		
		pane.setCenter(listItems);
		pane.setStyle("-fx-font-family: 'serif'");
		return new Scene(pane, 500, 500);
		
		
	}

	private String ChatLogs(Object data) {
		Message message;
		try {
			message = (Message) data;
		}
		catch (ClassCastException e) {
			return "[SERVER] " + data.toString();
		}

		String type = message.getMessageType();
		String sender = message.getUserID();
		String receiver = message.getReceiverID();
		String group = message.getGroupID();
		String body = message.getMessageBody();

		if (message.getStatusCode() == 400) {
			return "[ERROR] " + body;
		}

		if (Message.JOIN.equals(type)) {
			return "[JOIN] " + sender + ": " + body;
		}
		if (Message.SEND_ALL.equals(type)) {
			return "[ALL] " + sender + ": " + body;
		}
		if (Message.SEND_ONE.equals(type)) {
			return "[DM] " + sender + " -> " + receiver + ": " + body;
		}
		if (Message.CREATE_GROUP.equals(type)) {
			return "[GROUP CREATE] " + group + ": " + body;
		}
		if (Message.SEND_GROUP.equals(type)) {
			return "[GROUP] " + sender + " -> " + group + ": " + body;
		}
		if (Message.VIEW_USERS.equals(type)) {
			return "[USERS] " + body;
		}

		return "[MSG] " + body;
	}


}
