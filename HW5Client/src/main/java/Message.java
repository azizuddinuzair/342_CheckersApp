import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 42L;

	private int statusCode;
	private String messageType;
	private String userID;
	private String receiverID;
	private String messageBody;

	public static final String JOIN = "join";
	public static final String SEND_ONE = "send_one";

	public Message() {
	}

	public Message(String messageType, String userID, String receiverID, String messageBody) {
		this.messageType = messageType;
		this.userID = userID;
		this.receiverID = receiverID;
		this.messageBody = messageBody;
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
}