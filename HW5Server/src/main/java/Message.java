import java.io.Serializable;


/*
Instructions:
– Allow the user to pick a username when joining the server. The client should
receive an error message if the name is already taken and be prompted to provide
a new name. Once the client’s username is chosen, it should not be changable.
– View all users that are currently connected to the server
– Send a message either to all users or to an individual user.
– View all messages sent to the user.

Operations:
- Create a new user ID (join)
- Create a group (create_group)
- Send a message to all clients (send_all)
- Send a message to a group of clients (send_group)
- Send a message to an individual client (send_one)
- View all users (view_users)



The Message code will need to include all information that may be sent over the
network. The message needs to be able to: create a new user ID, create a group, send
a message to all clients, a group of clients or an individual

Variables for message class (kind of like HTTP header):
- Status code (200 for success, 400 for bad request, etc.)
- Message type (e.g., "join", "message", "list_users", etc.)
- User ID
- Receiver ID (for messages sent to an individual user)
- Group ID
- Message Body (the actual message content)

*/






/*
Uzair Azizuddin
Edits Made:
- Added operations for the Message class
- Added variables for the Message class
- Added methods for the Message class (setters, getters, and constructors)
*/

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    // Data each message needs to have
    private int statusCode;
    private String messageType;
    private String userID;
    private String receiverID;
    private String groupID;
    private String messageBody;


    // Message operations
    public static final String JOIN = "join";
    public static final String VIEW_USERS = "view_users";
    public static final String SEND_ALL = "send_all";
    public static final String SEND_ONE = "send_one";
    public static final String CREATE_GROUP = "create_group";
    public static final String SEND_GROUP = "send_group";



    // Methods
    public Message() {
    }

    // Constructor for creating a message with all fields.
    public Message(String messageType, String userID, String receiverID, String groupID, String messageBody) {
        this.messageType = messageType;
        this.userID = userID;
        this.receiverID = receiverID;
        this.groupID = groupID;
        this.messageBody = messageBody;
    }

    // Get status code of the message
    public int getStatusCode() {
        return statusCode;
    }

    // Set status code of the message to what is passed in
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }


    // Get message type of the message
    public String getMessageType() {
        return messageType;
    }

    // Set message type of the message to what is passed in
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    // Get user ID of the message
    public String getUserID() {
        return userID;
    }

    // Set user ID of the message to what is passed in
    public void setUserID(String userID) {
        this.userID = userID;
    }

    // Get receiver ID of the message
    public String getReceiverID() {
        return receiverID;
    }

    // Set receiver ID of the message to what is passed in
    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    // Get group ID of the message
    public String getGroupID() {
        return groupID;
    }

    // Set group ID of the message to what is passed in
    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    // Get message body of the message
    public String getMessageBody() {
        return messageBody;
    }

    // Set message body of the message to what is passed in
    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
