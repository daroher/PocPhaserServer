package server;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/game")
public class GameWebSocketServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("Nueva conexión: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session sender) {
        System.out.println("Mensaje recibido: " + message);
        broadcast(message, sender);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("Conexión cerrada: " + session.getId());
    }

    private void broadcast(String message, Session sender) {
        for (Session session : sessions) {
            if (session.isOpen() && !session.equals(sender)) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
