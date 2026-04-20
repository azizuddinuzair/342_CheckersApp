import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class Server {

	private int count = 1;
	private int gameCounter = 1;
	private ClientThread waitingClient;

	private ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	private HashSet<String> usernames = new HashSet<String>();
	private HashMap<String, ClientThread> clientsByUser = new HashMap<String, ClientThread>();
	private HashMap<String, GameSession> gamesById = new HashMap<String, GameSession>();
	private HashMap<String, GameSession> gamesByUser = new HashMap<String, GameSession>();

	private TheServer server;
	private Consumer<Serializable> callback;

	Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	private boolean isUniqueUserID(String userID) {
		return !usernames.contains(userID);
	}

	private void sendMessage(ClientThread clientThread, Message message) {
		if (clientThread == null || clientThread.out == null) {
			return;
		}
		try {
			clientThread.out.writeObject(message);
		}
		catch (Exception e) {
		}
	}

	private String boardToString(char[][] board) {
		StringBuilder builder = new StringBuilder();
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				builder.append(board[row][col]);
			}
			if (row < 7) {
				builder.append('/');
			}
		}
		return builder.toString();
	}

	private char[][] createStartingBoard() {
		char[][] board = new char[8][8];
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				board[row][col] = '.';
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 8; col++) {
				if ((row + col) % 2 == 1) {
					board[row][col] = 'r';
				}
			}
		}

		for (int row = 5; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				if ((row + col) % 2 == 1) {
					board[row][col] = 'b';
				}
			}
		}

		return board;
	}

	private String colorOfUser(GameSession game, String userID) {
		if (game.redPlayer.userID.equals(userID)) {
			return "red";
		}
		if (game.blackPlayer.userID.equals(userID)) {
			return "black";
		}
		return null;
	}

	private ClientThread opponentOf(GameSession game, String userID) {
		if (game.redPlayer.userID.equals(userID)) {
			return game.blackPlayer;
		}
		if (game.blackPlayer.userID.equals(userID)) {
			return game.redPlayer;
		}
		return null;
	}

	private boolean inBounds(int row, int col) {
		return row >= 0 && row < 8 && col >= 0 && col < 8;
	}

	private boolean isPlayerPiece(char piece, String color) {
		if ("red".equals(color)) {
			return piece == 'r' || piece == 'R';
		}
		return piece == 'b' || piece == 'B';
	}

	private boolean isOpponentPiece(char piece, String color) {
		if ("red".equals(color)) {
			return piece == 'b' || piece == 'B';
		}
		return piece == 'r' || piece == 'R';
	}

	private boolean isKing(char piece) {
		return piece == 'R' || piece == 'B';
	}

	private boolean hasAnyLegalMove(char[][] board, String color) {
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				char piece = board[row][col];
				if (!isPlayerPiece(piece, color)) {
					continue;
				}

				int[] directions = isKing(piece) ? new int[] { -1, 1 } : ("red".equals(color) ? new int[] { 1 } : new int[] { -1 });
				for (int i = 0; i < directions.length; i++) {
					int dr = directions[i];
					for (int dc = -1; dc <= 1; dc += 2) {
						int normalRow = row + dr;
						int normalCol = col + dc;
						if (inBounds(normalRow, normalCol) && board[normalRow][normalCol] == '.') {
							return true;
						}

						int jumpRow = row + (2 * dr);
						int jumpCol = col + (2 * dc);
						int middleRow = row + dr;
						int middleCol = col + dc;
						if (inBounds(jumpRow, jumpCol) && board[jumpRow][jumpCol] == '.' && inBounds(middleRow, middleCol)
								&& isOpponentPiece(board[middleRow][middleCol], color)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private String winnerByPieces(char[][] board) {
		int redCount = 0;
		int blackCount = 0;
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				if (board[row][col] == 'r' || board[row][col] == 'R') {
					redCount++;
				}
				if (board[row][col] == 'b' || board[row][col] == 'B') {
					blackCount++;
				}
			}
		}
		if (redCount == 0) {
			return "black";
		}
		if (blackCount == 0) {
			return "red";
		}
		return null;
	}

	private boolean applyMove(GameSession game, Message moveMessage, String playerColor, StringBuilder errorMessage) {
		int fromRow = moveMessage.getFromRow();
		int fromCol = moveMessage.getFromCol();
		int toRow = moveMessage.getToRow();
		int toCol = moveMessage.getToCol();
		char[][] board = game.board;

		if (!inBounds(fromRow, fromCol) || !inBounds(toRow, toCol)) {
			errorMessage.append("Move is out of bounds");
			return false;
		}

		if (board[toRow][toCol] != '.') {
			errorMessage.append("Destination is not empty");
			return false;
		}

		if ((toRow + toCol) % 2 == 0) {
			errorMessage.append("Move must land on a dark square");
			return false;
		}

		char piece = board[fromRow][fromCol];
		if (!isPlayerPiece(piece, playerColor)) {
			errorMessage.append("Selected piece is not yours");
			return false;
		}

		int deltaRow = toRow - fromRow;
		int deltaCol = toCol - fromCol;

		if (Math.abs(deltaRow) != Math.abs(deltaCol)) {
			errorMessage.append("Move must be diagonal");
			return false;
		}

		boolean king = isKing(piece);
		int direction = "red".equals(playerColor) ? 1 : -1;

		if (Math.abs(deltaRow) == 1) {
			if (!king && deltaRow != direction) {
				errorMessage.append("Piece can only move forward");
				return false;
			}
		}
		else if (Math.abs(deltaRow) == 2) {
			if (!king && deltaRow != 2 * direction) {
				errorMessage.append("Piece can only capture forward");
				return false;
			}
			int middleRow = (fromRow + toRow) / 2;
			int middleCol = (fromCol + toCol) / 2;
			char middlePiece = board[middleRow][middleCol];
			if (!isOpponentPiece(middlePiece, playerColor)) {
				errorMessage.append("No opponent piece to capture");
				return false;
			}
			board[middleRow][middleCol] = '.';
		}
		else {
			errorMessage.append("Move distance is invalid");
			return false;
		}

		board[toRow][toCol] = board[fromRow][fromCol];
		board[fromRow][fromCol] = '.';

		if (board[toRow][toCol] == 'r' && toRow == 7) {
			board[toRow][toCol] = 'R';
		}
		if (board[toRow][toCol] == 'b' && toRow == 0) {
			board[toRow][toCol] = 'B';
		}

		return true;
	}

	private void sendBoardState(GameSession game, String messageBody) {
		Message redView = new Message();
		redView.setMessageType(Message.BOARD_STATE);
		redView.setStatusCode(200);
		redView.setGameID(game.gameID);
		redView.setPlayerColor("red");
		redView.setOpponentID(game.blackPlayer.userID);
		redView.setBoardState(boardToString(game.board));
		redView.setTurnColor(game.turnColor);
		redView.setMessageBody(messageBody);

		Message blackView = new Message();
		blackView.setMessageType(Message.BOARD_STATE);
		blackView.setStatusCode(200);
		blackView.setGameID(game.gameID);
		blackView.setPlayerColor("black");
		blackView.setOpponentID(game.redPlayer.userID);
		blackView.setBoardState(boardToString(game.board));
		blackView.setTurnColor(game.turnColor);
		blackView.setMessageBody(messageBody);

		sendMessage(game.redPlayer, redView);
		sendMessage(game.blackPlayer, blackView);
	}

	private void endGame(GameSession game, String winnerColor, String reason) {
		game.active = false;
		gamesByUser.remove(game.redPlayer.userID);
		gamesByUser.remove(game.blackPlayer.userID);
		gamesById.remove(game.gameID);

		Message redEnd = new Message();
		redEnd.setMessageType(Message.GAME_OVER);
		redEnd.setStatusCode(200);
		redEnd.setGameID(game.gameID);
		redEnd.setPlayerColor("red");
		redEnd.setOpponentID(game.blackPlayer.userID);
		redEnd.setBoardState(boardToString(game.board));
		redEnd.setTurnColor(game.turnColor);
		if ("draw".equals(winnerColor)) {
			redEnd.setMessageBody("Game over: draw. " + reason);
		}
		else if ("red".equals(winnerColor)) {
			redEnd.setMessageBody("Game over: you win. " + reason);
		}
		else {
			redEnd.setMessageBody("Game over: you lose. " + reason);
		}

		Message blackEnd = new Message();
		blackEnd.setMessageType(Message.GAME_OVER);
		blackEnd.setStatusCode(200);
		blackEnd.setGameID(game.gameID);
		blackEnd.setPlayerColor("black");
		blackEnd.setOpponentID(game.redPlayer.userID);
		blackEnd.setBoardState(boardToString(game.board));
		blackEnd.setTurnColor(game.turnColor);
		if ("draw".equals(winnerColor)) {
			blackEnd.setMessageBody("Game over: draw. " + reason);
		}
		else if ("black".equals(winnerColor)) {
			blackEnd.setMessageBody("Game over: you win. " + reason);
		}
		else {
			blackEnd.setMessageBody("Game over: you lose. " + reason);
		}

		sendMessage(game.redPlayer, redEnd);
		sendMessage(game.blackPlayer, blackEnd);
		callback.accept("Game ended: " + game.gameID + " winner=" + winnerColor);
	}

	private void tryStartMatch(ClientThread secondPlayer) {
		if (waitingClient == null) {
			waitingClient = secondPlayer;
			Message waiting = new Message();
			waiting.setMessageType(Message.WAITING);
			waiting.setStatusCode(200);
			waiting.setMessageBody("Waiting for another player...");
			sendMessage(secondPlayer, waiting);
			return;
		}

		ClientThread firstPlayer = waitingClient;
		waitingClient = null;

		GameSession game = new GameSession();
		game.gameID = "game-" + gameCounter;
		gameCounter++;
		game.redPlayer = firstPlayer;
		game.blackPlayer = secondPlayer;
		game.board = createStartingBoard();
		game.turnColor = "red";
		game.active = true;

		gamesById.put(game.gameID, game);
		gamesByUser.put(firstPlayer.userID, game);
		gamesByUser.put(secondPlayer.userID, game);

		Message redMatch = new Message();
		redMatch.setMessageType(Message.MATCH_FOUND);
		redMatch.setStatusCode(200);
		redMatch.setGameID(game.gameID);
		redMatch.setPlayerColor("red");
		redMatch.setOpponentID(secondPlayer.userID);
		redMatch.setBoardState(boardToString(game.board));
		redMatch.setTurnColor(game.turnColor);
		redMatch.setMessageBody("Match found. You are RED. RED moves first.");

		Message blackMatch = new Message();
		blackMatch.setMessageType(Message.MATCH_FOUND);
		blackMatch.setStatusCode(200);
		blackMatch.setGameID(game.gameID);
		blackMatch.setPlayerColor("black");
		blackMatch.setOpponentID(firstPlayer.userID);
		blackMatch.setBoardState(boardToString(game.board));
		blackMatch.setTurnColor(game.turnColor);
		blackMatch.setMessageBody("Match found. You are BLACK. RED moves first.");

		sendMessage(firstPlayer, redMatch);
		sendMessage(secondPlayer, blackMatch);
		callback.accept("Started match: " + game.gameID + " red=" + firstPlayer.userID + " black=" + secondPlayer.userID);
	}

	private void handleChat(ClientThread sender, Message chatMessage) {
		GameSession game = gamesByUser.get(sender.userID);
		if (game == null || !game.active) {
			Message error = new Message();
			error.setMessageType(Message.ERROR);
			error.setStatusCode(400);
			error.setMessageBody("You are not in an active game");
			sendMessage(sender, error);
			return;
		}

		ClientThread opponent = opponentOf(game, sender.userID);
		if (opponent == null) {
			Message error = new Message();
			error.setMessageType(Message.ERROR);
			error.setStatusCode(400);
			error.setMessageBody("Opponent not available");
			sendMessage(sender, error);
			return;
		}

		Message toOpponent = new Message();
		toOpponent.setMessageType(Message.CHAT);
		toOpponent.setStatusCode(200);
		toOpponent.setGameID(game.gameID);
		toOpponent.setUserID(sender.userID);
		toOpponent.setReceiverID(opponent.userID);
		toOpponent.setMessageBody(sender.userID + ": " + chatMessage.getMessageBody());
		sendMessage(opponent, toOpponent);

		Message toSender = new Message();
		toSender.setMessageType(Message.CHAT);
		toSender.setStatusCode(200);
		toSender.setGameID(game.gameID);
		toSender.setUserID(sender.userID);
		toSender.setReceiverID(opponent.userID);
		toSender.setMessageBody("You: " + chatMessage.getMessageBody());
		sendMessage(sender, toSender);
	}

	private void handleMove(ClientThread sender, Message moveMessage) {
		GameSession game = gamesByUser.get(sender.userID);
		if (game == null || !game.active) {
			Message error = new Message();
			error.setMessageType(Message.ERROR);
			error.setStatusCode(400);
			error.setMessageBody("You are not in an active game");
			sendMessage(sender, error);
			return;
		}

		String playerColor = colorOfUser(game, sender.userID);
		if (playerColor == null) {
			Message error = new Message();
			error.setMessageType(Message.ERROR);
			error.setStatusCode(400);
			error.setMessageBody("You are not part of this game");
			sendMessage(sender, error);
			return;
		}

		if (!playerColor.equals(game.turnColor)) {
			Message error = new Message();
			error.setMessageType(Message.ERROR);
			error.setStatusCode(400);
			error.setMessageBody("It is not your turn");
			sendMessage(sender, error);
			return;
		}

		StringBuilder errorMessage = new StringBuilder();
		boolean applied = applyMove(game, moveMessage, playerColor, errorMessage);
		if (!applied) {
			Message error = new Message();
			error.setMessageType(Message.ERROR);
			error.setStatusCode(400);
			error.setMessageBody(errorMessage.toString());
			sendMessage(sender, error);
			return;
		}

		game.turnColor = "red".equals(game.turnColor) ? "black" : "red";
		sendBoardState(game, sender.userID + " moved");

		String winnerByPieces = winnerByPieces(game.board);
		if (winnerByPieces != null) {
			endGame(game, winnerByPieces, "All opponent pieces captured.");
			return;
		}

		if (!hasAnyLegalMove(game.board, game.turnColor)) {
			String winner = "red".equals(game.turnColor) ? "black" : "red";
			endGame(game, winner, "Opponent has no legal moves.");
		}
	}

	private void handleDisconnect(ClientThread clientThread) {
		if (clientThread.userID != null) {
			usernames.remove(clientThread.userID);
			clientsByUser.remove(clientThread.userID);
		}
		clients.remove(clientThread);

		if (waitingClient == clientThread) {
			waitingClient = null;
		}

		if (clientThread.userID == null) {
			return;
		}

		GameSession game = gamesByUser.get(clientThread.userID);
		if (game != null && game.active) {
			ClientThread opponent = opponentOf(game, clientThread.userID);
			if (opponent != null) {
				Message gameOver = new Message();
				gameOver.setMessageType(Message.GAME_OVER);
				gameOver.setStatusCode(200);
				gameOver.setGameID(game.gameID);
				gameOver.setBoardState(boardToString(game.board));
				gameOver.setMessageBody("Game over: opponent disconnected.");
				sendMessage(opponent, gameOver);
			}
			gamesByUser.remove(game.redPlayer.userID);
			gamesByUser.remove(game.blackPlayer.userID);
			gamesById.remove(game.gameID);
		}
	}

	public class GameSession {
		String gameID;
		ClientThread redPlayer;
		ClientThread blackPlayer;
		char[][] board;
		String turnColor;
		boolean active;
	}

	public class ClientThread extends Thread {

		Socket connection;
		int count;
		String userID;
		ObjectInputStream in;
		ObjectOutputStream out;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		private String handleJoinRequest() {
			try {
				Message joinRequest = (Message) in.readObject();
				String requestedUserID = joinRequest.getUserID();

				if (requestedUserID == null || requestedUserID.equals("")) {
					Message errorMsg = new Message();
					errorMsg.setMessageType(Message.ERROR);
					errorMsg.setStatusCode(400);
					errorMsg.setMessageBody("Username is required");
					sendMessage(this, errorMsg);
					return null;
				}

				synchronized (Server.this) {
					if (!isUniqueUserID(requestedUserID)) {
						Message errorMsg = new Message();
						errorMsg.setMessageType(Message.ERROR);
						errorMsg.setStatusCode(400);
						errorMsg.setMessageBody("Username already taken");
						sendMessage(this, errorMsg);
						return null;
					}
					usernames.add(requestedUserID);
					userID = requestedUserID;
					clientsByUser.put(requestedUserID, this);
				}

				Message joinMsg = new Message();
				joinMsg.setMessageType(Message.JOIN);
				joinMsg.setStatusCode(200);
				joinMsg.setUserID(requestedUserID);
				joinMsg.setMessageBody("Joined server as " + requestedUserID);
				sendMessage(this, joinMsg);
				callback.accept("Joined: " + requestedUserID);
				return requestedUserID;
			}
			catch (Exception e) {
				return null;
			}
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);
			}
			catch (Exception e) {
				callback.accept("Streams not open");
				return;
			}

			String joinedUserID = handleJoinRequest();
			if (joinedUserID == null) {
				handleDisconnect(this);
				return;
			}

			synchronized (Server.this) {
				tryStartMatch(this);
			}

			while (true) {
				try {
					Message data = (Message) in.readObject();
					synchronized (Server.this) {
						if (Message.CHAT.equals(data.getMessageType())) {
							handleChat(this, data);
						}
						else if (Message.MOVE.equals(data.getMessageType())) {
							handleMove(this, data);
						}
					}
				}
				catch (Exception e) {
					synchronized (Server.this) {
						callback.accept("Client disconnected: " + joinedUserID);
						handleDisconnect(this);
					}
					break;
				}
			}
		}
	}

	public class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				callback.accept("Server is waiting for a client!");
				while (true) {
					Socket socket = mysocket.accept();
					ClientThread clientThread = new ClientThread(socket, count);
					callback.accept("Socket connected");
					synchronized (Server.this) {
						clients.add(clientThread);
					}
					clientThread.start();
					count++;
				}
			}
			catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}
	}
}
