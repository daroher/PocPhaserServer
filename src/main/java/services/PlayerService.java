package services;

import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import logica.Player;
import utils.NotificationHelper;
import utils.ServerEvents;
import vo.GameEvent;

public class PlayerService {
	private static final Gson gson = new Gson();
	private JsonObject bismarckLastPos = null;
	private static final long ADVANTAGE_COOLDOWN_MS = 15000; // 15 segundos de cooldown
	private long lastAdvantageTime = 0; // Última vez que se activó la ventaja

	public PlayerService() {
	}

	public void handleNewPlayer(Session senderSession, String data, Set<Session> sessions,
			Map<String, Player> players) {
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);

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

	public void handleMovePlayer(Session senderSession, String data, Map<String, Player> players) {
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
		String playerId = senderSession.getId();
		Player player = players.get(playerId);
		if (player != null) {
			player.setX(playerEvent.getX());
			player.setY(playerEvent.getY());
			player.setVisionRadius(playerEvent.getVisionRadius());
			player.setAngle(playerEvent.getAngle());
			player.setIsPlaneActive(playerEvent.isPlaneActive());
			checkMapVision(player, players);
		}
		for (Player otherPlayer : players.values()) {
			if (!otherPlayer.getSession().getId().equals(senderSession.getId())) {
				NotificationHelper.sendMessage(otherPlayer.getSession(), data);
			}
		}
	}

	private void checkMapVision(Player player, Map<String, Player> players) {
		for (Player otherPlayer : players.values()) {
			if (otherPlayer.getSession().getId().equals(player.getSession().getId()))
				continue;

			float distance = (float) Math.sqrt(
					Math.pow(player.getX() - otherPlayer.getX(), 2) + Math.pow(player.getY() - otherPlayer.getY(), 2));

			// Si está dentro del rango de visión
			if (distance != 0 && distance <= player.getVisionRadius()) {
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

			// Verificar la visión inversa
			if (distance != 0 && distance <= otherPlayer.getVisionRadius()) {
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
			double distance = Math
					.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2));

			JsonObject messageInRange = new JsonObject();
			messageInRange.addProperty("action", ServerEvents.JUGADOR_EN_RANGO);
			messageInRange.addProperty("x", target.getX());
			messageInRange.addProperty("y", target.getY());
			messageInRange.addProperty("team", target.getTeam());
			messageInRange.addProperty("angle", target.getAngle());
			messageInRange.addProperty("distance", distance);
			NotificationHelper.sendMessage(observer.getSession(), messageInRange.toString());

			JsonObject bismarckPos = new JsonObject();
			if (observer.isWithOperator()) {
				bismarckPos.addProperty("x", target.getX());
				bismarckPos.addProperty("y", target.getY());
				this.bismarckLastPos = bismarckPos;
			} else if (target.isWithOperator()) {
				bismarckPos.addProperty("x", observer.getX());
				bismarckPos.addProperty("y", observer.getY());
				this.bismarckLastPos = bismarckPos;
			}

			if (observer.getTeam().equals("bismarck")) {
				boolean bismarckUsedAdvantage = observer.hasBismarckUsedAdvantage();

				if (!bismarckUsedAdvantage && !target.isWithObserver()) {
					JsonObject messageVentaja = new JsonObject();
					messageVentaja.addProperty("action", ServerEvents.INICIA_VENTAJA);
					messageVentaja.addProperty("startTeam", observer.getTeam());
					messageVentaja.addProperty("otherTeam", target.getTeam());
					messageVentaja.addProperty("distance", distance);
					NotificationHelper.sendMessage(observer.getSession(), messageVentaja.toString());
					NotificationHelper.sendMessage(target.getSession(), messageVentaja.toString());

					observer.setBismarckUsedAdvantage(true);
				} else if (bismarckUsedAdvantage) {

					JsonObject guerraMessage = new JsonObject();
					guerraMessage.addProperty("action", ServerEvents.INICIA_GUERRA);
					guerraMessage.addProperty("startTeam", observer.getTeam());
					guerraMessage.addProperty("otherTeam", target.getTeam());
					guerraMessage.addProperty("distance", distance);
					NotificationHelper.sendMessage(observer.getSession(), guerraMessage.toString());
					NotificationHelper.sendMessage(target.getSession(), guerraMessage.toString());
				}
			} else if (observer.getTeam().equals("britanicos") && observer.isWithObserver()
					&& observer.isPlaneActive()) {
				JsonObject guerraMessage = new JsonObject();
				guerraMessage.addProperty("action", ServerEvents.INICIA_GUERRA);
				guerraMessage.addProperty("startTeam", observer.getTeam());
				guerraMessage.addProperty("otherTeam", target.getTeam());
				guerraMessage.addProperty("distance", distance);
				NotificationHelper.sendMessage(observer.getSession(), guerraMessage.toString());
				NotificationHelper.sendMessage(target.getSession(), guerraMessage.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void notifyPlayerOutOfRange(Player observer, Player target) {
		try {
			JsonObject message = new JsonObject();
			message.addProperty("action", ServerEvents.JUGADOR_FUERA_RANGO);
			message.addProperty("team", target.getTeam());
			NotificationHelper.sendMessage(observer.getSession(), message.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleMovePlayerWar(Session senderSession, String data, Map<String, Player> players) {
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

	public void handleMovePlayerSideview(Session senderSession, String data, Map<String, Player> players) {
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

	public void handleEndSideview(Session senderSession, String data, Map<String, Player> players) {
		for (Player player : players.values()) {
			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.VOLVER_VISTA_SUPERIOR);
			mensaje.addProperty("x", player.getX());
			mensaje.addProperty("y", player.getY());
			NotificationHelper.sendMessage(player.getSession(), mensaje.toString());
		}
	}

	public void handleConsultarPosicionBismarck(Session senderSession, String data) {
		try {
			if (bismarckLastPos != null) {
				JsonObject responseMessage = new JsonObject();
				responseMessage.addProperty("action", ServerEvents.DATOS_POSICION_BISMARCK);
				responseMessage.addProperty("x", bismarckLastPos.get("x").getAsFloat());
				responseMessage.addProperty("y", bismarckLastPos.get("y").getAsFloat());

				NotificationHelper.sendMessage(senderSession, responseMessage.toString());
				bismarckLastPos = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handleAvionSinCombustible(Session senderSession, String data, Map<String, Player> players,
			Set<Session> sessions) {
		try {
			String playerId = senderSession.getId();
			Player player = players.get(playerId);

			if (player != null) {
				if ("britanicos".equals(player.getTeam())) {
					int currentPlanes = GameStateService.getInstance().getCantAviones();
					if (currentPlanes > 0) {
						GameStateService.getInstance().setCantAviones(currentPlanes - 1);

						if (GameStateService.getInstance().getCantAviones() == 0) {
							GameStateService.getInstance().checkVictory(sessions, players);
						} else {
							sendVolverPortavionesMessage(senderSession, playerId);
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handleVolverPortaviones(Session senderSession, String data, Map<String, Player> players) {
		try {
			String playerId = senderSession.getId();
			Player player = players.get(playerId);

			if (player != null && "britanicos".equals(player.getTeam())) {
				sendVolverPortavionesMessage(senderSession, playerId);

			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	private void sendVolverPortavionesMessage(Session session, String playerId) {
		JsonObject returnMessage = new JsonObject();
		returnMessage.addProperty("action", ServerEvents.VOLVER_PORTAVIONES);
		returnMessage.addProperty("playerId", playerId);

		NotificationHelper.sendMessage(session, returnMessage.toString());
	}

}
