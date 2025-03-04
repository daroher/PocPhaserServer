package services;

import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import logica.GameEvent;
import logica.Player;
import utils.NotificationHelper;
import utils.ServerEvents;

public class PlayerService {
	private static final Gson gson = new Gson();
	private Map<String, Player> players;

	public PlayerService(Map<String, Player> players) {
		this.players = players;
	}

	public void handleNewPlayer(Session senderSession, String data, Set<Session> sessions) {
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
		System.out.println("selecciono:" + playerEvent.isWithObserver());

		Player player = new Player(senderSession.getId(), playerEvent.getTeam(), playerEvent.getX(), playerEvent.getY(),
				playerEvent.getVisionRadius(), senderSession, playerEvent.getAngle());
		players.put(senderSession.getId(), player);

		// Se asignan parámetros iniciales según el equipo (podría delegarse en otro
		// servicio)
		if ("bismarck".equals(player.getTeam())) {
			GameStateService.getInstance().setVidaBismarck(3);
		} else {
			GameStateService.getInstance().setCantAviones(10);
		}

		for (Session session : sessions) {
			if (!session.getId().equals(senderSession.getId())) {
				NotificationHelper.sendMessage(session, data);
			}
		}
	}

	public void handleMovePlayer(Session senderSession, String data) {
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
		String playerId = senderSession.getId();
		Player player = players.get(playerId);

		if (player != null) {
			player.setX(playerEvent.getX());
			player.setY(playerEvent.getY());
			player.setVisionRadius(playerEvent.getVisionRadius());
			player.setAngle(playerEvent.getAngle());
			// Actualizamos la visión del mapa para este jugador
			VisionService.getInstance().checkMapVision(player, players);
		}

		// Notificar movimiento a los demás jugadores
		for (Player otherPlayer : players.values()) {
			if (!otherPlayer.getSession().getId().equals(senderSession.getId())) {
				NotificationHelper.sendMessage(otherPlayer.getSession(), data);
			}
		}
	}

	public void handleMovePlayerWar(Session senderSession, String data) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			String team = playerEvent.getTeam();
			float x = playerEvent.getX();
			float y = playerEvent.getY();
			float vsDistanceZ = playerEvent.getVsDistanceZ();

			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.MUEVO_JUGADOR_GUERRA);
					positionMessage.addProperty("team", team);
					positionMessage.addProperty("x", x);
					positionMessage.addProperty("y", y);
					positionMessage.addProperty("vsDistanceZ", vsDistanceZ);

					NotificationHelper.sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handleMovePlayerSideview(Session senderSession, String data) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			String team = playerEvent.getTeam();
			float x = playerEvent.getX();
			float y = playerEvent.getY();
			float angle = playerEvent.getAngle();

			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.MUEVO_JUGADOR_VENTAJA);
					positionMessage.addProperty("team", team);
					positionMessage.addProperty("x", x);
					positionMessage.addProperty("y", y);
					positionMessage.addProperty("angle", angle);

					NotificationHelper.sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handleEndSideview(Session senderSession, String data) {
		for (Player player : players.values()) {
			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.VOLVER_VISTA_SUPERIOR);
			mensaje.addProperty("x", player.getX());
			mensaje.addProperty("y", player.getY());
			NotificationHelper.sendMessage(player.getSession(), mensaje.toString());
		}
	}
}
