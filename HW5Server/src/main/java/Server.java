import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;


import java.util.HashSet;

import javafx.application.Platform;
import javafx.scene.control.ListView;
/*
 * Clicker: A: I really get it    
B: No idea what you are talking about
 * C: kind of following
 */



/*
Uzair Azizuddin
Edits Made:
- Updated String to Message objects instea
- change the join to send message not string
- Implemented a hashset to store usernames for immediate lookup and checking of unique usernames
- Implemented a method to check if a username is unique
- Replaced count with userID from Message class
- added duplicate usename error message to client
- Created method to handle all kinds of message types, and created method for each message type operation
*/

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	HashMap<String, HashSet<String>> groups = new HashMap<String, HashSet<String>>();
	TheServer server;
	private Consumer<Serializable> callback;


	HashSet<String> usernames = new HashSet<String>();
	
	
	Server(Consumer<Serializable> call){
	
		callback = call;
		server = new TheServer();
		server.start();
		groups.put("server_group", new HashSet<String>());
	}

	//Check user id is unique
	public synchronized boolean checkUniqueID(String userID) {
    	if (usernames.contains(userID)){
    		return false;
    	}
    	return true;
	}	
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				callback.accept("socket connected, waiting for username");
				synchronized (Server.this) {
					clients.add(c);
				}
				c.start();
				
				count++;
				
			    }
			}//end of try
				catch(Exception e) {
					callback.accept("Server socket did not launch");
				}
			}//end of while
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			String userID;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
			}
			
			// Method to send a message to all clients
			public synchronized void updateClients(Message message) {
				for(int i = 0; i < clients.size(); i++) {
					ClientThread clientThread = clients.get(i);
					try {
					 clientThread.out.writeObject(message);
					}
					catch(Exception e) {}
				}
			}

			// Method to handle a join request from a client and return the user ID if the join request is successful, otherwise return null
			private String handleJoinRequest() {
				try {
					Message joinRequest = (Message) in.readObject();
					String requestedUserID = joinRequest.getUserID();

					if(checkUniqueID(requestedUserID)) {
						synchronized (Server.this) {
							usernames.add(requestedUserID);
						}
						userID = requestedUserID;
						synchronized (Server.this) {
							groups.get("server_group").add(requestedUserID);
						}

						Message joinMsg = new Message();
						joinMsg.setMessageType(Message.JOIN);
						joinMsg.setStatusCode(200);
						joinMsg.setUserID(requestedUserID);
						joinMsg.setMessageBody(requestedUserID + " joined the server");
						updateClients(joinMsg);
						callback.accept(joinMsg);
						return requestedUserID;
					}

					Message errorMsg = new Message();
					errorMsg.setMessageType(Message.SEND_ONE);
					errorMsg.setStatusCode(400);
					errorMsg.setMessageBody("Username already taken");
					out.writeObject(errorMsg);
					callback.accept(errorMsg);
					return null;
				}
				catch(Exception e) {
					return null;
				}
			}

			// Method to create a group chat
			private void createGroup(Message message) {
				HashSet<String> members = new HashSet<String>();
				String groupID = message.getGroupID();
				String memberID = message.getMessageBody();

				if (memberID != null && !memberID.equals("")) {
					members.add(memberID);
				}

				members.add(message.getUserID());

				synchronized (Server.this) {
					groups.put(groupID, members);
				}

				Message groupMsg = new Message();
				groupMsg.setMessageType(Message.CREATE_GROUP);
				groupMsg.setGroupID(groupID);
				groupMsg.setMessageBody(groupID + " created");
				updateClients(groupMsg);
			}

			// Method to send a message to one specific user
			private void sendToOne(Message message) {
				for (int i = 0; i < clients.size(); i++) {
					ClientThread clientThread = clients.get(i);
					if (clientThread.userID != null && clientThread.userID.equals(message.getReceiverID())) {
						try {
							clientThread.out.writeObject(message);
						}
						catch(Exception e) {}
					}
				}
			}

			// Method to send a list of all connected users
			private void sendUserList() {
				Message userListMsg = new Message();
				userListMsg.setMessageType(Message.VIEW_USERS);
				userListMsg.setStatusCode(200);
				userListMsg.setReceiverID(userID);
				userListMsg.setMessageBody(usernames.toString());
				sendToOne(userListMsg);
			}

			// Method to send a message to a group
			private void sendToGroup(Message message) {
				HashSet<String> members;
				synchronized (Server.this) {
					members = groups.get(message.getGroupID());
				}
				if (members == null) {
					return;
				}

				for (int i = 0; i < clients.size(); i++) {
					ClientThread clientThread = clients.get(i);
					if (clientThread.userID != null && members.contains(clientThread.userID)) {
						try {
							clientThread.out.writeObject(message);
						}
						catch(Exception e) {}
					}
				}
			}

			// Method to decide what message operation to do based on message type
			private void handleMessage(Message message) {
				callback.accept(message);

				//Operations:
				// - Create a group (create_group)
				// - Send a message to all clients (send_all)
				// - Send a message to a group of clients (send_group)
				// - Send a message to an individual client (send_one)
				// - View all users (view_users)
				switch(message.getMessageType()) {
					case Message.SEND_ALL:
						updateClients(message);
						break;
					case Message.SEND_ONE:
						sendToOne(message);
						break;
					case Message.CREATE_GROUP:
						createGroup(message);
						break;
					case Message.SEND_GROUP:
						sendToGroup(message);
						break;
					case Message.VIEW_USERS:
						sendUserList();
						break;
				}
					
			}
			
			public void run(){
					
				try {
					out = new ObjectOutputStream(connection.getOutputStream());
					in = new ObjectInputStream(connection.getInputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}
				
				String userID = handleJoinRequest();
				if (userID == null) {
					return;
				}
					
				 while(true) {
					    try {
							// Read a message from the client and send it to all clients
					    	Message data = (Message) in.readObject();
					    	callback.accept(userID + " sent: " + data.getMessageBody());
					    	handleMessage(data);
					    	
					    	}
					    catch(Exception e) {
							// If there is an error with the socket, send a message to all clients that the client has left the server and remove the client
					    	callback.accept("OOOOPPs...Something wrong with the socket from client: " + userID + "....closing down!");
					    	Message leftMsg = new Message();
					    	leftMsg.setMessageType(Message.SEND_ALL);
					    	leftMsg.setUserID(userID);
					    	leftMsg.setMessageBody(userID + " has left the server!");
					    	updateClients(leftMsg);
					    	synchronized (Server.this) {
					    		usernames.remove(userID);
					    		clients.remove(this);
					    	}
					    	break;
					    }
					}
				}//end of run
			
			
		}//end of client thread
}


	
	

	
