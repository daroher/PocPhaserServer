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
	private static final Map<String, Player> players = Collections.synchronizedMap(new HashMap<>());
	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

	@OnOpen
	public void onOpen(Session session) {
		sessions.add(session);

		try {
			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.JUGADORES_ACTUALES);

			JsonArray jugadores = new JsonArray();
			for (Player player : players.values()) {
				JsonObject jugador = new JsonObject();
				jugador.addProperty("team", player.getTeam());
				jugadores.add(jugador);
			}

			mensaje.add("jugadores", jugadores);

			sendMessage(session, mensaje.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@OnMessage
	public void onMessage(String message, Session senderSession) {
		try {
			GameEvent player = gson.fromJson(message, GameEvent.class);
			if (player == null || player.getAction() == null) {
				System.err.println("Error: mensaje inválido.");
				return;
			}
			messageReducer(player.getAction(), senderSession, message);
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);

		String playerId = session.getId();
		Player player = players.get(playerId);

		if (player != null) {
			String teamName = player.getTeam();
			players.remove(playerId);

			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.JUGADOR_DESCONECTADO);
			mensaje.addProperty("team", teamName);

			for (Player otherPlayer : players.values()) {
				if (!otherPlayer.getSession().getId().equals(playerId)) {
					sendMessage(otherPlayer.getSession(), mensaje.toString());
				}
			}

			for (Session activeSession : sessions) {
				if (!activeSession.getId().equals(playerId)) {
					sendMessage(activeSession, mensaje.toString());
				}
			}
		}
	}

	private void messageReducer(String action, Session senderSession, String data) {
		switch (action) {
		case ServerEvents.NUEVO_JUGADOR:
			handleNewPlayer(senderSession, data);
			break;
		case ServerEvents.MUEVO_JUGADOR:
			handleMovePlayer(senderSession, data);
			break;
		case ServerEvents.LLEGA_FRANCIA:
			handleFindFrance(senderSession, data);
			break;
		case ServerEvents.DISPARO_ACERTADO:
			handleShoot(senderSession, data);
			break;
		case ServerEvents.MUEVO_JUGADOR_GUERRA:
			handleMovePlayerWar(senderSession, data);
			break;
		case ServerEvents.DISPARO_BALA_BISMARCK:
			handleBismarckBullet(senderSession, data);
			break;
		case ServerEvents.DISPARO_BALA_AVION:
			handlePlaneBullet(senderSession, data);
			break;

		default:
			System.err.println("Acción no reconocida: " + action);
			break;
		}
	}

	private void handleNewPlayer(Session senderSession, String data) {
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
		Player player = new Player(senderSession.getId(), playerEvent.getTeam(), playerEvent.getX(), playerEvent.getY(),
				playerEvent.getVisionRadius(), senderSession, playerEvent.getAngle());
		players.put(senderSession.getId(), player);

		for (Session session : sessions) {
			if (!session.getId().equals(senderSession.getId())) {
				sendMessage(session, data);
			}
		}

		if (players.size() >= 2) {
			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.INICIAR_PARTIDA);

			for (Player otherPlayer : players.values()) {
				sendMessage(otherPlayer.getSession(), mensaje.toString());
			}
		}
	}

	private void handleMovePlayer(Session senderSession, String data) {
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
		String playerId = senderSession.getId();
		Player player = players.get(playerId);

		if (player != null) {
			player.setX(playerEvent.getX());
			player.setY(playerEvent.getY());
			player.setVisionRadius(playerEvent.getVisionRadius());
			player.setAngle(playerEvent.getAngle());
			checkMapVision(player);
		}

		for (Player otherPlayer : players.values()) {
			if (!otherPlayer.getSession().getId().equals(senderSession.getId())) {
				sendMessage(otherPlayer.getSession(), data);
			}
		}
	}

	private void checkMapVision(Player player) {
		for (Player otherPlayer : players.values()) {
			if (otherPlayer.getSession().getId().equals(player.getSession().getId())) {
				continue; // Ignorar al mismo jugador
			}

			float distance = (float) Math.sqrt(
					Math.pow(player.getX() - otherPlayer.getX(), 2) + Math.pow(player.getY() - otherPlayer.getY(), 2));

			// Verificar si el jugador actual está dentro del rango del otro jugador
			if (distance <= player.getVisionRadius()) {
				if (!player.isInVisionRangeOf(otherPlayer)) {
					player.setInVisionRangeOf(otherPlayer, true);
					notifyPlayerInRange(player, otherPlayer);
				}
			} else {
				if (player.isInVisionRangeOf(otherPlayer)) {
					player.setInVisionRangeOf(otherPlayer, false);
					notifyPlayerOutOfRange(player, otherPlayer);
				}
			}

			// Verificar si el otro jugador está dentro del rango del jugador actual
			if (distance <= otherPlayer.getVisionRadius()) {
				if (!otherPlayer.isInVisionRangeOf(player)) {
					otherPlayer.setInVisionRangeOf(player, true);
					notifyPlayerInRange(otherPlayer, player);
				}
			} else {
				if (otherPlayer.isInVisionRangeOf(player)) {
					otherPlayer.setInVisionRangeOf(player, false);
					notifyPlayerOutOfRange(otherPlayer, player);
				}
			}
		}
	}

	private void notifyPlayerInRange(Player observer, Player target) {
		try {
			JsonObject message = new JsonObject();
			message.addProperty("action", ServerEvents.JUGADOR_EN_RANGO);
			message.addProperty("x", target.getX());
			message.addProperty("y", target.getY());
			message.addProperty("team", target.getTeam());
			message.addProperty("angle", target.getAngle());
			message.addProperty("distance", Math
					.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2)));

			sendMessage(observer.getSession(), message.toString());

			// Mensaje de guerra
			JsonObject guerraMessage = new JsonObject();
			guerraMessage.addProperty("action", ServerEvents.INICIA_GUERRA);
			guerraMessage.addProperty("startTeam", observer.getTeam());
			guerraMessage.addProperty("otherTeam", target.getTeam());
			guerraMessage.addProperty("distance", Math
					.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2)));

			sendMessage(observer.getSession(), guerraMessage.toString());
			sendMessage(target.getSession(), guerraMessage.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void notifyPlayerOutOfRange(Player observer, Player target) {
		try {
			JsonObject message = new JsonObject();
			message.addProperty("action", ServerEvents.JUGADOR_FUERA_RANGO);
			message.addProperty("team", target.getTeam());

			sendMessage(observer.getSession(), message.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendMessage(Session session, String message) {
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

	private void handleFindFrance(Session senderSession, String data) {
		GameEventFrance playerEvent = gson.fromJson(data, GameEventFrance.class);
		String playerId = senderSession.getId();
		Player player = players.get(playerId);

		if (player != null) {
			if (player.getX() == playerEvent.getX() && player.getY() == playerEvent.getY()
					&& player.getAngle() == playerEvent.getAngle()
					&& player.getVisionRadius() == playerEvent.getVisionRadius()) {

				float distance = (float) Math.sqrt(Math.pow(playerEvent.getX() - playerEvent.getFranceX(), 2)
						+ Math.pow(playerEvent.getY() - playerEvent.getFranceY(), 2));

				if (distance <= player.getVisionRadius()) {
					JsonObject message = new JsonObject();
					message.addProperty("action", ServerEvents.GANA_PARTIDA);
					message.addProperty("team", playerEvent.getTeam());

					for (Player p : players.values()) {
						sendMessage(p.getSession(), message.toString());
					}
				}

			} else {
				System.out.println("Desincronización de coordenadas para el jugador: " + playerId);
			}
		}

	}

	private void handleShoot(Session senderSession, String data) {
		try {
			GameEvent shootEvent = gson.fromJson(data, GameEvent.class);
			String team = shootEvent.getTeam();

			JsonObject victoryMessage = new JsonObject();
			victoryMessage.addProperty("action", ServerEvents.GANA_PARTIDA);
			victoryMessage.addProperty("team", team);

			for (Player player : players.values()) {
				Session session = player.getSession();
				if (session.isOpen()) {
					sendMessage(session, victoryMessage.toString());
				}
			}

			players.clear();
			sessions.clear();
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	private void handleMovePlayerWar(Session senderSession, String data) {
		try {

			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			String team = playerEvent.getTeam();
			float x = playerEvent.getX();
			float y = playerEvent.getY();

			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {

					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.MUEVO_JUGADOR_GUERRA);
					positionMessage.addProperty("team", team);
					positionMessage.addProperty("x", x);
					positionMessage.addProperty("y", y);

					sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
		}

	}

	private void handleBismarckBullet(Session senderSession, String data) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			float angle = playerEvent.getRelativeAngle();
			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.DISPARO_BALA_BISMARCK);
					positionMessage.addProperty("angle", angle);
					sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	private void handlePlaneBullet(Session senderSession, String data) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			float angle = playerEvent.getRelativeAngle();
			float x = playerEvent.getX();
			float y = playerEvent.getY();
			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.DISPARO_BALA_AVION);
					positionMessage.addProperty("angle", angle);
					positionMessage.addProperty("x", x);
					positionMessage.addProperty("y", y);
					sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

}
