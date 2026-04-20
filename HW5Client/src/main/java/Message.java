import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 42L;

	public static final String JOIN = "join";
	public static final String WAITING = "waiting";
	public static final String MATCH_FOUND = "match_found";
	public static final String CHAT = "chat";
	public static final String MOVE = "move";
	public static final String BOARD_STATE = "board_state";
	public static final String GAME_OVER = "game_over";
	public static final String ERROR = "error";
	public static final String REMATCH = "rematch";
	public static final String QUIT = "quit";

	private int statusCode;
	private String messageType;
	private String userID;
	private String receiverID;
	private String messageBody;

	private String gameID;
	private String playerColor;
	private String opponentID;
	private String boardState;
	private String turnColor;

	private int fromRow;
	private int fromCol;
	private int toRow;
	private int toCol;

	public Message() {
		fromRow = -1;
		fromCol = -1;
		toRow = -1;
		toCol = -1;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getReceiverID() {
		return receiverID;
	}

	public void setReceiverID(String receiverID) {
		this.receiverID = receiverID;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	public String getGameID() {
		return gameID;
	}

	public void setGameID(String gameID) {
		this.gameID = gameID;
	}

	public String getPlayerColor() {
		return playerColor;
	}

	public void setPlayerColor(String playerColor) {
		this.playerColor = playerColor;
	}

	public String getOpponentID() {
		return opponentID;
	}

	public void setOpponentID(String opponentID) {
		this.opponentID = opponentID;
	}

	public String getBoardState() {
		return boardState;
	}

	public void setBoardState(String boardState) {
		this.boardState = boardState;
	}

	public String getTurnColor() {
		return turnColor;
	}

	public void setTurnColor(String turnColor) {
		this.turnColor = turnColor;
	}

	public int getFromRow() {
		return fromRow;
	}

	public void setFromRow(int fromRow) {
		this.fromRow = fromRow;
	}

	public int getFromCol() {
		return fromCol;
	}

	public void setFromCol(int fromCol) {
		this.fromCol = fromCol;
	}

	public int getToRow() {
		return toRow;
	}

	public void setToRow(int toRow) {
		this.toRow = toRow;
	}

	public int getToCol() {
		return toCol;
	}

	public void setToCol(int toCol) {
		this.toCol = toCol;
	}
}
