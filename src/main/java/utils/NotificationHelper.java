package utils;

import java.io.IOException;

import javax.websocket.Session;

public class NotificationHelper {
	public static void sendMessage(Session session, String message) {
		synchronized (session) {
			try {
				if (session.isOpen()) {
					session.getBasicRemote().sendText(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
