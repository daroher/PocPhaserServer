package server;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

@ServerEndpoint("/game")
public class WebSocketFacade {
	private static final Gson gson = new Gson();
	  // Se utiliza el singleton del servicio para mantener el estado compartido
    private static final GameService gameService = GameService.getInstance();

    @OnOpen
    public void onOpen(Session session) {
        gameService.onOpen(session);
    }

    @OnMessage
    public void onMessage(String message, Session senderSession) {
        gameService.onMessage(message, senderSession);
    }

    @OnClose
    public void onClose(Session session) {
        gameService.onClose(session);
    }
}
