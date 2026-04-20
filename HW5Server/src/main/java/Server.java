import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Consumer;

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;
	HashSet<String> usernames = new HashSet<String>();

	Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	public synchronized boolean checkUniqueID(String userID) {
		return !usernames.contains(userID);
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

		private void sendToUser(String targetUserID, Message message) {
			for (int i = 0; i < clients.size(); i++) {
				ClientThread clientThread = clients.get(i);
				if (clientThread.userID != null && clientThread.userID.equals(targetUserID)) {
					try {
						clientThread.out.writeObject(message);
					}
					catch (Exception e) {
					}
				}
			}
		}

		private void sendDirectMessage(Message message) {
			String senderID = message.getUserID();
			String receiverID = message.getReceiverID();
			String body = message.getMessageBody();

			if (receiverID == null || receiverID.equals("")) {
				Message errorMsg = new Message();
				errorMsg.setMessageType(Message.SEND_ONE);
				errorMsg.setStatusCode(400);
				errorMsg.setMessageBody("Receiver username is required");
				sendToUser(senderID, errorMsg);
				return;
			}

			Message senderView = new Message();
			senderView.setMessageType(Message.SEND_ONE);
			senderView.setUserID(senderID);
			senderView.setReceiverID(receiverID);
			senderView.setStatusCode(200);
			senderView.setMessageBody("To " + receiverID + ": " + body);

			Message receiverView = new Message();
			receiverView.setMessageType(Message.SEND_ONE);
			receiverView.setUserID(senderID);
			receiverView.setReceiverID(receiverID);
			receiverView.setStatusCode(200);
			receiverView.setMessageBody(senderID + ": " + body);

			boolean receiverFound = false;
			for (int i = 0; i < clients.size(); i++) {
				ClientThread clientThread = clients.get(i);
				if (clientThread.userID != null && clientThread.userID.equals(senderID)) {
					try {
						clientThread.out.writeObject(senderView);
					}
					catch (Exception e) {
					}
				}
				if (clientThread.userID != null && clientThread.userID.equals(receiverID)) {
					try {
						clientThread.out.writeObject(receiverView);
						receiverFound = true;
					}
					catch (Exception e) {
					}
				}
			}

			if (!receiverFound) {
				Message errorMsg = new Message();
				errorMsg.setMessageType(Message.SEND_ONE);
				errorMsg.setStatusCode(404);
				errorMsg.setMessageBody("Receiver not found");
				sendToUser(senderID, errorMsg);
			}
			else {
				callback.accept(senderID + " sent a direct message to " + receiverID);
			}
		}

		private String handleJoinRequest() {
			try {
				Message joinRequest = (Message) in.readObject();
				String requestedUserID = joinRequest.getUserID();

				if (checkUniqueID(requestedUserID)) {
					synchronized (Server.this) {
						usernames.add(requestedUserID);
					}
					userID = requestedUserID;

					Message joinMsg = new Message();
					joinMsg.setMessageType(Message.JOIN);
					joinMsg.setStatusCode(200);
					joinMsg.setUserID(requestedUserID);
					joinMsg.setMessageBody(requestedUserID + " joined the server");
					out.writeObject(joinMsg);
					callback.accept(joinMsg);
					return requestedUserID;
				}

				Message errorMsg = new Message();
				errorMsg.setMessageType(Message.JOIN);
				errorMsg.setStatusCode(400);
				errorMsg.setMessageBody("Username already taken");
				out.writeObject(errorMsg);
				callback.accept(errorMsg);
				return null;
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
				return;
			}

			while (true) {
				try {
					Message data = (Message) in.readObject();
					if (Message.SEND_ONE.equals(data.getMessageType())) {
						sendDirectMessage(data);
					}
				}
				catch (Exception e) {
					callback.accept("Client disconnected: " + joinedUserID);
					synchronized (Server.this) {
						usernames.remove(joinedUserID);
						clients.remove(this);
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
					callback.accept("socket connected");
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