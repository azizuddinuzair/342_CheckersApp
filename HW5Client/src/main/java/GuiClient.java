
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

public class GuiClient extends Application{

	
	TextField c1;
	TextField c2;
	Button b1;
	HashMap<String, Scene> sceneMap;
	VBox clientBox;
	Client clientConnection;
	boolean joined = false;
	String userID;


	// Extra features: button to send private message, button to create group, button to send message to group, and button to view all users in server
	Button privateMsgButton;
	Button createGroupButton;
	Button sendGroupButton;
	Button viewUsersButton;

	
	ListView<String> listItems2;
	
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems2 = new ListView<String>();
		
		c1 = new TextField();
		c1.setPromptText("Enter username, press Send. After join, type message here");
		c2 = new TextField();
		c2.setPromptText("Choose user/group");
		b1 = new Button("Send");
		b1.setOnAction(e->{handleSend();});


		privateMsgButton = new Button("Send Private Msg");
		privateMsgButton.setOnAction(e->{
			if (joined) {
				sendPrivateMessage(c2.getText(), c1.getText());
				c1.clear();
			}
		});
		createGroupButton = new Button("Create Group");
		createGroupButton.setOnAction(e->{
			if (joined) {
				createGroup(c2.getText(), c1.getText());
				c1.clear();
			}
		});
		sendGroupButton = new Button("Send Group Msg");
		sendGroupButton.setOnAction(e->{
			if (joined) {
				sendGroupMessage(c2.getText(), c1.getText());
				c1.clear();
			}
		});
		viewUsersButton = new Button("View All Users");
		viewUsersButton.setOnAction(e->{
			if (joined) {
				viewUsers();
			}
		});

		clientConnection = new Client(data->{
				Platform.runLater(()->{
					Message message = (Message) data;
					if (message.getStatusCode() == 400) {
						joined = false;
						userID = null;
					}
					if (Message.JOIN.equals(message.getMessageType()) && userID != null && userID.equals(message.getUserID())) {
						joined = true;
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

		listItems2.getItems().add("Instructions:");
		listItems2.getItems().add("1) Join: type username in top box and click Send");
		listItems2.getItems().add("2) Create group: bottom box = group name, top box = member username, click Create Group");
		listItems2.getItems().add("3) Send group msg: bottom box = group name, top box = message, click Send Group Msg");
		listItems2.getItems().add("4) View users: click View All Users");
		
		sceneMap = new HashMap<String, Scene>();

		sceneMap.put("client",  createClientGui());
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });


		primaryStage.setScene(sceneMap.get("client"));
		primaryStage.setTitle("Client");
		primaryStage.show();
		
	}
	

	
	public Scene createClientGui() {
		
		HBox topButtons = new HBox(10, b1, privateMsgButton, viewUsersButton);
		HBox bottomButtons = new HBox(10, createGroupButton, sendGroupButton);
		clientBox = new VBox(10, c1, c2, topButtons, bottomButtons, listItems2);
		clientBox.setStyle("-fx-background-color: blue;"+"-fx-font-family: 'serif';");
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
			sendChatMessage(input);
		}

		c1.clear();
	}

	// methods send private message, create group, send message to group, and view users methods to be implemented here
	private void sendPrivateMessage(String receiverID, String text) {
		if (receiverID.isEmpty() || text.isEmpty()) {
			return;
		}
		Message message = new Message();
		message.setMessageType(Message.SEND_ONE);
		message.setUserID(userID);
		message.setReceiverID(receiverID);
		message.setMessageBody(text);
		clientConnection.send(message);
	}

	private void createGroup(String groupID, String memberID) {
		if (groupID.isEmpty() || memberID.isEmpty()) {
			return;
		}
		Message message = new Message();
		message.setMessageType(Message.CREATE_GROUP);
		message.setUserID(userID);
		message.setGroupID(groupID);
		message.setMessageBody(memberID);
		clientConnection.send(message);
	}

	private void sendGroupMessage(String groupID, String text) {
		if (groupID.isEmpty() || text.isEmpty()) {
			return;
		}
		Message message = new Message();
		message.setMessageType(Message.SEND_GROUP);
		message.setUserID(userID);
		message.setGroupID(groupID);
		message.setMessageBody(text);
		clientConnection.send(message);
	}

	private void viewUsers() {
		Message message = new Message();
		message.setMessageType(Message.VIEW_USERS);
		message.setUserID(userID);
		clientConnection.send(message);
	}


	private void sendJoinMessage(String requestedUserID) {
		Message message = new Message();
		message.setMessageType(Message.JOIN);
		message.setUserID(requestedUserID);
		clientConnection.send(message);
	}

	private void sendChatMessage(String text) {
		Message message = new Message();
		message.setMessageType(Message.SEND_ALL);
		message.setUserID(userID);
		message.setMessageBody(text);
		clientConnection.send(message);
	}

}
