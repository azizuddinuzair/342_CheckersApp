import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GuiClient extends Application {

	private HashMap<String, Scene> sceneMap;
	private Client clientConnection;

	private TextField usernameField;
	private Button joinButton;
	private Label statusLabel;

	private GridPane boardGrid;
	private Button[][] boardButtons;
	private char[][] currentBoard;

	private ListView<String> chatList;
	private TextField chatField;
	private Button chatSendButton;

	private boolean joined = false;
	private boolean inGame = false;
	private String userID;
	private String playerColor;
	private String opponentID;
	private String gameID;
	private String turnColor;

	private int selectedRow = -1;
	private int selectedCol = -1;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		sceneMap = new HashMap<String, Scene>();
		setupUi();

		clientConnection = new Client(data -> Platform.runLater(() -> handleServerMessage((Message) data)));
		clientConnection.start();

		sceneMap.put("client", createClientGui());
		primaryStage.setScene(sceneMap.get("client"));
		primaryStage.setTitle("Checkers Client");
		primaryStage.show();
	}

	private void setupUi() {
		usernameField = new TextField();
		usernameField.setPromptText("Enter username");

		joinButton = new Button("Join");
		joinButton.setOnAction(e -> handleJoin());

		statusLabel = new Label("Join the server to begin.");

		boardGrid = new GridPane();
		boardGrid.setHgap(0);
		boardGrid.setVgap(0);
		boardGrid.setAlignment(Pos.CENTER);
		boardButtons = new Button[8][8];
		currentBoard = emptyBoard();

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				final int finalRow = row;
				final int finalCol = col;
				Button square = new Button();
				square.setMinSize(64, 64);
				square.setMaxSize(64, 64);
				square.setOnAction(e -> handleBoardClick(finalRow, finalCol));
				boardButtons[row][col] = square;
				boardGrid.add(square, col, row);
			}
		}

		chatList = new ListView<String>();
		chatList.setPrefHeight(150);

		chatField = new TextField();
		chatField.setPromptText("Type message to opponent");

		chatSendButton = new Button("Send Chat");
		chatSendButton.setOnAction(e -> handleChatSend());
	}

	private Scene createClientGui() {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(10));

		HBox topRow = new HBox(10, usernameField, joinButton, statusLabel);
		topRow.setAlignment(Pos.CENTER_LEFT);
		root.setTop(topRow);

		root.setCenter(boardGrid);

		HBox chatInputRow = new HBox(10, chatField, chatSendButton);
		chatInputRow.setAlignment(Pos.CENTER_LEFT);
		VBox bottomPanel = new VBox(8, chatList, chatInputRow);
		bottomPanel.setPadding(new Insets(10, 0, 0, 0));
		root.setBottom(bottomPanel);

		renderBoard();
		return new Scene(root, 760, 760);
	}

	private void handleJoin() {
		if (joined) {
			statusLabel.setText("Already joined as " + userID);
			return;
		}

		String requestedUserID = usernameField.getText();
		if (requestedUserID == null || requestedUserID.equals("")) {
			statusLabel.setText("Username is required.");
			return;
		}

		Message message = new Message();
		message.setMessageType(Message.JOIN);
		message.setUserID(requestedUserID);
		clientConnection.send(message);
		statusLabel.setText("Joining as " + requestedUserID + "...");
	}

	private void handleChatSend() {
		if (!inGame) {
			statusLabel.setText("Chat is available only during an active match.");
			return;
		}

		String text = chatField.getText();
		if (text == null || text.equals("")) {
			return;
		}

		Message message = new Message();
		message.setMessageType(Message.CHAT);
		message.setUserID(userID);
		message.setReceiverID(opponentID);
		message.setGameID(gameID);
		message.setMessageBody(text);
		clientConnection.send(message);
		chatField.clear();
	}

	private void handleBoardClick(int row, int col) {
		if (!inGame) {
			statusLabel.setText("Waiting for match...");
			return;
		}

		if (turnColor == null || !turnColor.equals(playerColor)) {
			statusLabel.setText("Not your turn.");
			return;
		}

		char clickedPiece = currentBoard[row][col];
		boolean ownPiece = isOwnPiece(clickedPiece);

		if (selectedRow == -1) {
			if (!ownPiece) {
				statusLabel.setText("Select one of your pieces.");
				return;
			}
			selectedRow = row;
			selectedCol = col;
			statusLabel.setText("Piece selected. Choose destination.");
			renderBoard();
			return;
		}

		if (ownPiece) {
			selectedRow = row;
			selectedCol = col;
			statusLabel.setText("Piece re-selected. Choose destination.");
			renderBoard();
			return;
		}

		Message move = new Message();
		move.setMessageType(Message.MOVE);
		move.setUserID(userID);
		move.setGameID(gameID);
		move.setFromRow(selectedRow);
		move.setFromCol(selectedCol);
		move.setToRow(row);
		move.setToCol(col);
		clientConnection.send(move);

		selectedRow = -1;
		selectedCol = -1;
		renderBoard();
	}

	private void handleServerMessage(Message message) {
		String type = message.getMessageType();
		if (type == null) {
			return;
		}

		if (Message.JOIN.equals(type)) {
			if (message.getStatusCode() == 200) {
				joined = true;
				userID = message.getUserID();
				usernameField.setDisable(true);
				joinButton.setDisable(true);
				statusLabel.setText(message.getMessageBody());
			}
			else {
				statusLabel.setText(message.getMessageBody());
			}
			return;
		}

		if (Message.WAITING.equals(type)) {
			statusLabel.setText(message.getMessageBody());
			return;
		}

		if (Message.MATCH_FOUND.equals(type)) {
			inGame = true;
			gameID = message.getGameID();
			playerColor = message.getPlayerColor();
			opponentID = message.getOpponentID();
			turnColor = message.getTurnColor();
			loadBoard(message.getBoardState());
			selectedRow = -1;
			selectedCol = -1;
			renderBoard();
			statusLabel.setText(message.getMessageBody());
			chatList.getItems().clear();
			chatList.getItems().add("Matched with " + opponentID + ".");
			return;
		}

		if (Message.BOARD_STATE.equals(type)) {
			turnColor = message.getTurnColor();
			loadBoard(message.getBoardState());
			renderBoard();
			selectedRow = -1;
			selectedCol = -1;
			statusLabel.setText(message.getMessageBody() + " Turn: " + turnColor);
			return;
		}

		if (Message.CHAT.equals(type)) {
			chatList.getItems().add(message.getMessageBody());
			return;
		}

		if (Message.GAME_OVER.equals(type)) {
			inGame = false;
			turnColor = null;
			loadBoard(message.getBoardState());
			renderBoard();
			statusLabel.setText(message.getMessageBody());
			chatList.getItems().add(message.getMessageBody());
			return;
		}

		if (Message.ERROR.equals(type)) {
			statusLabel.setText(message.getMessageBody());
			return;
		}
	}

	private char[][] emptyBoard() {
		char[][] board = new char[8][8];
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				board[row][col] = '.';
			}
		}
		return board;
	}

	private void loadBoard(String boardState) {
		if (boardState == null || boardState.equals("")) {
			return;
		}
		String[] rows = boardState.split("/");
		if (rows.length != 8) {
			return;
		}
		for (int row = 0; row < 8; row++) {
			if (rows[row].length() != 8) {
				return;
			}
			for (int col = 0; col < 8; col++) {
				currentBoard[row][col] = rows[row].charAt(col);
			}
		}
	}

	private boolean isOwnPiece(char piece) {
		if ("red".equals(playerColor)) {
			return piece == 'r' || piece == 'R';
		}
		if ("black".equals(playerColor)) {
			return piece == 'b' || piece == 'B';
		}
		return false;
	}

	private void renderBoard() {
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				Button square = boardButtons[row][col];
				boolean darkSquare = (row + col) % 2 == 1;
				String baseColor = darkSquare ? "#769656" : "#EEEED2";

				if (row == selectedRow && col == selectedCol) {
					baseColor = "#f6f669";
				}

				square.setStyle("-fx-background-color: " + baseColor + "; -fx-font-size: 24px; -fx-font-weight: bold;");

				char piece = currentBoard[row][col];
				if (piece == '.') {
					square.setText("");
				}
				else if (piece == 'r') {
					square.setText("r");
				}
				else if (piece == 'R') {
					square.setText("R");
				}
				else if (piece == 'b') {
					square.setText("b");
				}
				else if (piece == 'B') {
					square.setText("B");
				}
			}
		}
	}
}
