package services;

import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import logica.GameEvent;
import logica.GameEventFrance;
import logica.Player;
import utils.NotificationHelper;
import utils.ServerEvents;

public class GameStateService {
	private int cantAviones = 10;
	private int vidaBismarck = 3;

	private static GameStateService instance;

	private GameStateService() {
	}

	public static GameStateService getInstance() {
		if (instance == null) {
			instance = new GameStateService();
		}
		return instance;
	}

	public int getCantAviones() {
		return cantAviones;
	}

	public void setCantAviones(int cantAviones) {
		this.cantAviones = cantAviones;
	}

	public void decrementAviones() {
		if (cantAviones > 0) {
			cantAviones--;
		}
	}

	public int getVidaBismarck() {
		return vidaBismarck;
	}

	public void setVidaBismarck(int vidaBismarck) {
		this.vidaBismarck = vidaBismarck;
	}

	public void decrementVidaBismarck() {
		if (vidaBismarck > 0) {
			vidaBismarck--;
		}
	}

	public void handleFindFrance(Session senderSession, String data, Map<String, Player> players) {
		GameEventFrance playerEvent = new Gson().fromJson(data, GameEventFrance.class);
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
						NotificationHelper.sendMessage(p.getSession(), message.toString());
					}
				}

			} else {
				System.out.println("Desincronización de coordenadas para el jugador: " + playerId);
			}
		}
	}

	public void handleAircraftCarrierPositionSelection(Session senderSession, String data,
			Map<String, Player> players) {
		try {
			GameEvent playerEvent = new Gson().fromJson(data, GameEvent.class);
			float x = playerEvent.getX();
			float y = playerEvent.getY();
			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					if (players.size() >= 2) {
						JsonObject mensaje = new JsonObject();
						mensaje.addProperty("action", ServerEvents.INICIAR_PARTIDA);
						mensaje.addProperty("carrierX", x);
						mensaje.addProperty("carrierY", y);
						for (Player otherPlayer : players.values()) {
							NotificationHelper.sendMessage(otherPlayer.getSession(), mensaje.toString());
						}
					}					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void checkVictory(Set<Session> sessions, Map<String, Player> players) {
		if (cantAviones == 0 || vidaBismarck == 0) {
			String team = (cantAviones == 0) ? "bismarck" : "britanicos";
			sendVictoryMessage(team, sessions, players);
		}
	}

	private void sendVictoryMessage(String team, Set<Session> sessions, Map<String, Player> players) {
		JsonObject victoryMessage = new JsonObject();
		victoryMessage.addProperty("action", ServerEvents.GANA_PARTIDA);
		victoryMessage.addProperty("team", team);

		for (Player player : players.values()) {
			if (player.getSession().isOpen()) {
				NotificationHelper.sendMessage(player.getSession(), victoryMessage.toString());
			}
		}

		players.clear();
		sessions.clear();
	}
}
