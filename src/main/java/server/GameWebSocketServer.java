package server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@ServerEndpoint("/game")
public class GameWebSocketServer {
	private static final Gson gson = new Gson();

	private static final Map<String, String> players = Collections.synchronizedMap(new HashMap<>());

	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

	@OnOpen
	public void onOpen(Session session) {
		sessions.add(session);
		System.out.println("Jugador conectado: " + session.getId());

		try {
			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", "jugadores_actuales");

			JsonArray jugadores = new JsonArray();
			for (String playerId : players.keySet()) { // Itera sobre las claves del mapa (session.getId())
				String teamName = players.get(playerId); // Obtiene el nombre del equipo
				JsonObject jugador = new JsonObject();
				jugador.addProperty("team", teamName); // Añade el equipo al mensaje
				jugadores.add(jugador);
			}

			mensaje.add("jugadores", jugadores);

			session.getBasicRemote().sendText(mensaje.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void messageReducer(String action, Session senderSession, String data) {
		switch (action) {
		case ServerEvents.NUEVO_JUGADOR: {
			GameEvent player = gson.fromJson(data, GameEvent.class);
			players.put(senderSession.getId(), player.getTeam());

			synchronized (sessions) {
				for (Session session : sessions) {
					if (session.isOpen() && !session.equals(senderSession)) {
						try {
							session.getBasicRemote().sendText(data);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				if (players.size() >= 2) {
					JsonObject mensaje = new JsonObject();
					mensaje.addProperty("action", ServerEvents.INICIAR_PARTIDA);

					for (Session session : sessions) {
						if (session.isOpen()) {
							try {
								session.getBasicRemote().sendText(mensaje.toString());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			break;
		}
		case ServerEvents.MUEVO_JUGADOR: {
		    System.out.println("Mueve " + data);

		    synchronized (sessions) {
		        for (Session session : sessions) {
		            if (session.isOpen() && !session.equals(senderSession)) { // No reenvíes al mismo jugador
		                try {
		                    session.getBasicRemote().sendText(data); // Enviar la actualización a los demás jugadores
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		        }
		    }
		    break;
		}
		
		default:
			System.err.println("Acción no reconocida: " + action);
			break;
		}
	}

	@OnMessage
	public void onMessage(String message, Session senderSession) {
		System.out.println("Mensaje recibido: " + message);
		System.out.println("senderSession: " + senderSession.getId());

		try {
			GameEvent player = gson.fromJson(message, GameEvent.class);
			String playerAction = player.getAction();

			if (playerAction != null) {
				messageReducer(playerAction, senderSession, message);
			} else {
				System.err.println("Error: sin action.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);
		String playerId = session.getId();

		String teamName = players.get(playerId);

		players.remove(playerId);

		if (teamName != null) {
			System.out.println("Jugador desconectado: " + playerId + " del equipo: " + teamName);

			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", "jugador_desconectado");
			mensaje.addProperty("team", teamName); // Añade el nombre del equipo

			for (Session s : sessions) {
				if (!s.getId().equals(playerId)) {
					try {
						s.getBasicRemote().sendText(mensaje.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}
}
