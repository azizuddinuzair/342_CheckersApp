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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
	private Label redCount;
	private Label blackCount;
	private Label turnLabel;

	private VBox redCaptured;
	private VBox blackCaptured;

	private GridPane boardGrid;
	private Button[][] boardButtons;
	private char[][] currentBoard;

	private ListView<String> chatList;
	private TextField chatField;
	private Button chatSendButton;
	private Button playAgainButton;
	private Button quitButton;

	private boolean joined = false;
	private boolean inGame = false;
	private String userID;
	private String playerColor;
	private String opponentID;
	private String gameID;
	private String turnColor;

	private Image redPieceImage;
	private Image redKingImage;
	private Image blackPieceImage;
	private Image blackKingImage;

	private int selectedRow = -1;
	private int selectedCol = -1;

	private boolean inBounds(int row, int col) {
		return row >= 0 && row < 8 && col >= 0 && col < 8;
	}

	private boolean isKing(char piece) {
		return piece == 'R' || piece == 'B';
	}

	private boolean isPlayerPiece(char piece, String color) {
		if ("red".equalsIgnoreCase(color)) return piece == 'r' || piece == 'R';
		if ("black".equalsIgnoreCase(color)) return piece == 'b' || piece == 'B';
		return false;
	}

	private boolean isOpponentPiece(char piece, String color) {
		if ("red".equalsIgnoreCase(color)) return piece == 'b' || piece == 'B';
		if ("black".equalsIgnoreCase(color)) return piece == 'r' || piece == 'R';
		return false;
	}

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
		redCount = new Label("Red pieces: 12");
		blackCount = new Label("Black pieces: 12");
		turnLabel = new Label("Turn: Waiting");

		redCaptured = new VBox(5);
		blackCaptured = new VBox(5);

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
		chatField.setPrefWidth(200);
		chatField.setPromptText("Type message to opponent");

		chatSendButton = new Button("Send Chat");
		chatSendButton.setOnAction(e -> handleChatSend());

		redPieceImage = new Image(getClass().getResourceAsStream("/checkers_images/red.png"));
		redKingImage = new Image(getClass().getResourceAsStream("/checkers_images/red_king.png"));
		blackPieceImage = new Image(getClass().getResourceAsStream("/checkers_images/black.png"));
		blackKingImage = new Image(getClass().getResourceAsStream("/checkers_images/black_king.png"));

		playAgainButton = new Button("Play Again");
		playAgainButton.setDisable(true);
		playAgainButton.setOnAction(e -> handlePlayAgain());

		quitButton = new Button("Quit");
		quitButton.setDisable(true);
		quitButton.setOnAction(e -> handleQuitGame());
	}

	private Scene createClientGui() {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(10));

		HBox topRow = new HBox(10, usernameField, joinButton, statusLabel, redCount, blackCount, turnLabel);
		topRow.setAlignment(Pos.CENTER_LEFT);
		root.setTop(topRow);

		root.setCenter(boardGrid);
		root.setLeft(redCaptured);
		root.setRight(blackCaptured);

		HBox chatInputRow = new HBox(10, chatField, chatSendButton, playAgainButton, quitButton);
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

		if (!isMoveValid(selectedRow, selectedCol, row, col)) {
			statusLabel.setText("Invalid move");
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
		statusLabel.setText("");
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
			statusLabel.setText("");
			renderBoard();
			statusLabel.setText(message.getMessageBody());
			chatList.getItems().clear();
			chatList.getItems().add("Matched with " + opponentID + ".");
			playAgainButton.setDisable(true);
			quitButton.setDisable(false);
			return;
		}

		if (Message.BOARD_STATE.equals(type)) {
			turnColor = message.getTurnColor();
			loadBoard(message.getBoardState());
			renderBoard();
			selectedRow = -1;
			selectedCol = -1;
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
			playAgainButton.setDisable(false);
			quitButton.setDisable(false);
			return;
		}

		if (Message.ERROR.equals(type)) {
			statusLabel.setText(message.getMessageBody());
			return;
		}
	}

	private void handlePlayAgain() {
		if (gameID == null || gameID.equals("")) {
			return;
		}
		Message message = new Message();
		message.setMessageType(Message.REMATCH);
		message.setUserID(userID);
		message.setGameID(gameID);
		clientConnection.send(message);
		playAgainButton.setDisable(true);
	}

	private void handleQuitGame() {
		Message message = new Message();
		message.setMessageType(Message.QUIT);
		message.setUserID(userID);
		message.setGameID(gameID);
		clientConnection.send(message);
		inGame = false;
		statusLabel.setText("You quit the game.");
		playAgainButton.setDisable(true);
		quitButton.setDisable(true);
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

	private boolean hasAnyCapture(char[][] board, String color) {
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				if (isPlayerPiece(board[row][col], color)) {
					if (hasCaptureFrom(board, row, col, color)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	private boolean hasCaptureFrom(char[][] board, int row, int col, String color) {
		char piece = board[row][col];
		int[] directions = isKing(piece) ? new int[] { -1, 1 } : ("red".equalsIgnoreCase(color) ? new int[] { 1 } : new int[] { -1 });

		for (int dr : directions) {
			for (int dc : new int[] {-1, 1}) {
				int midR = row + dr;
				int midC = col + dc;
				int jumpR = row + (2 * dr);
				int jumpC = col + (2 * dc);

				if (inBounds(jumpR, jumpC)) {
					if (board[jumpR][jumpC] == '.' && isOpponentPiece(board[midR][midC], color)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isMoveValid(int fromR, int fromC, int toR, int toC) {
		if (!inBounds(toR, toC) || currentBoard[toR][toC] != '.') return false;

		char piece = currentBoard[fromR][fromC];
		int rowDiff = toR - fromR;
		int colDiff = Math.abs(toC - fromC);

		boolean globalCaptureExists = hasAnyCapture(currentBoard, playerColor);

		if (colDiff == 2 && Math.abs(rowDiff) == 2) {
			int dr = rowDiff / 2;
			int dc = (toC - fromC) / 2;
			int midR = fromR + dr;
			int midC = fromC + dc;

			boolean validDir = isKing(piece) ||
					(playerColor.equalsIgnoreCase("red") && dr == 1) ||
					(playerColor.equalsIgnoreCase("black") && dr == -1);

			if (validDir && isOpponentPiece(currentBoard[midR][midC], playerColor)) {
				return true;
			}
		}

		if (!globalCaptureExists && colDiff == 1) {
			if (piece == 'r' && rowDiff == 1) return true;
			if (piece == 'b' && rowDiff == -1) return true;
			if (isKing(piece) && Math.abs(rowDiff) == 1) return true;
		}

		return false;
	}

	private boolean isOwnPiece(char piece) {
		if ("red".equalsIgnoreCase(playerColor)) {
			return piece == 'r' || piece == 'R';
		}
		if ("black".equalsIgnoreCase(playerColor)) {
			return piece == 'b' || piece == 'B';
		}
		return false;
	}

	private void updatePieceCounts() {
		int rCount = 0;
		int bCount = 0;
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				char piece = currentBoard[row][col];
				if (piece == 'r' || piece == 'R') rCount++;
				if (piece == 'b' || piece == 'B') bCount++;
			}
		}
		redCount.setText("Red: " + rCount);
		blackCount.setText("Black: " + bCount);
	}

	private void updateTurnIndicator() {
		if (turnColor == null) {
			turnLabel.setText("Turn: Waiting");
			turnLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
			return;
		}
		if (turnColor.equals("red")) {
			turnLabel.setText("Turn: RED");
			turnLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
		} else {
			turnLabel.setText("Turn: BLACK");
			turnLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
		}
	}

	private void updateCapturedPieces() {
		redCaptured.getChildren().clear();
		blackCaptured.getChildren().clear();

		redCaptured.getChildren().add(new Label("Red captured:"));
		blackCaptured.getChildren().add(new Label("Black captured:"));

		int redCount = 0;
		int blackCount = 0;

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				char piece = currentBoard[row][col];
				if (piece == 'r' || piece == 'R') redCount++;
				if (piece == 'b' || piece == 'B') blackCount++;
			}
		}

		int redCapturedNum = 12 - blackCount;
		int blackCapturedNum = 12 - redCount;

		for (int i = 0; i < redCapturedNum; i++) {
			ImageView img = new ImageView(blackPieceImage);
			img.setFitWidth(32);
			img.setFitHeight(32);
			img.setPreserveRatio(true);
			redCaptured.getChildren().add(img);
		}

		for (int i = 0; i < blackCapturedNum; i++) {
			ImageView img = new ImageView(redPieceImage);
			img.setFitWidth(32);
			img.setFitHeight(32);
			img.setPreserveRatio(true);
			blackCaptured.getChildren().add(img);
		}
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

				else if (selectedRow != -1 && isMoveValid(selectedRow, selectedCol, row, col)) {
					baseColor = "#99ff99";
				}

				square.setStyle("-fx-background-color: " + baseColor + "; -fx-boarder-color: rgba(0,0,0,0.1);");

				char piece = currentBoard[row][col];
				if (piece == '.') {
					square.setGraphic(null);
					square.setText("");
				}
				else {
					ImageView pieceGraphic = null;
					if (piece == 'r') {
						pieceGraphic = new ImageView(redPieceImage);
					}
					else if (piece == 'R') {
						pieceGraphic = new ImageView(redKingImage);
					}
					else if (piece == 'b') {
						pieceGraphic = new ImageView(blackPieceImage);
					}
					else if (piece == 'B') {
						pieceGraphic = new ImageView(blackKingImage);
					}

					if (pieceGraphic != null) {
						pieceGraphic.setFitWidth(88);
						pieceGraphic.setFitHeight(88);
						pieceGraphic.setPreserveRatio(true);
						square.setGraphic(pieceGraphic);
						square.setText("");
					}
					else {
						square.setGraphic(null);
						square.setText(String.valueOf(piece));
					}
				}
			}
		}
		updatePieceCounts();
		updateTurnIndicator();
		updateCapturedPieces();
	}
}
