import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application {

	TextField c1;
	TextField c2;
	Button b1;
	HashMap<String, Scene> sceneMap;
	VBox clientBox;
	Client clientConnection;
	boolean joined = false;
	String userID;

	ListView<String> listItems2;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems2 = new ListView<String>();

		c1 = new TextField();
		c1.setPromptText("Enter username to join, then type message here");
		c2 = new TextField();
		c2.setPromptText("Opponent username");
		b1 = new Button("Send");
		b1.setOnAction(e -> handleSend());

		clientConnection = new Client(data -> {
			Platform.runLater(() -> {
				Message message = (Message) data;
				if (message.getStatusCode() == 400) {
					joined = false;
					userID = null;
					c1.setPromptText("Enter username to join, then type message here");
				}
				if (Message.JOIN.equals(message.getMessageType()) && userID != null && userID.equals(message.getUserID())) {
					joined = true;
					c1.setPromptText("Type direct message here");
				}
				if (message.getMessageBody() != null) {
					listItems2.getItems().add(message.getMessageBody());
				}
				else {
					listItems2.getItems().add(message.toString());
				}
			});
		});

		clientConnection.start();
		listItems2.getItems().add("Join first, then send a direct message to your opponent.");

		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("client", createClientGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(sceneMap.get("client"));
		primaryStage.setTitle("Client");
		primaryStage.show();
	}

	public Scene createClientGui() {
		HBox topButtons = new HBox(10, b1);
		clientBox = new VBox(10, c1, c2, topButtons, listItems2);
		clientBox.setStyle("-fx-background-color: blue;" + "-fx-font-family: 'serif';");
		return new Scene(clientBox, 800, 500);
	}

	private void handleSend() {
		String input = c1.getText();
		if (input.isEmpty()) {
			return;
		}

		if (!joined) {
			sendJoinMessage(input);
			userID = input;
			listItems2.getItems().add("Attempting to join as: " + input);
		}
		else {
			String receiverID = c2.getText();
			if (receiverID.isEmpty()) {
				listItems2.getItems().add("Enter an opponent username in the second box.");
				return;
			}
			sendDirectMessage(receiverID, input);
		}

		c1.clear();
	}

	private void sendJoinMessage(String requestedUserID) {
		Message message = new Message();
		message.setMessageType(Message.JOIN);
		message.setUserID(requestedUserID);
		clientConnection.send(message);
	}

	private void sendDirectMessage(String receiverID, String text) {
		Message message = new Message();
		message.setMessageType(Message.SEND_ONE);
		message.setUserID(userID);
		message.setReceiverID(receiverID);
		message.setMessageBody(text);
		clientConnection.send(message);
	}
}