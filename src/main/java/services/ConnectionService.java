package services;

import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import logica.Player;
import utils.NotificationHelper;
import utils.ServerEvents;

public class ConnectionService {
	private Map<String, Player> players;

	public ConnectionService(Map<String, Player> players) {
		this.players = players;
	}

	public void handleNewConnection(Session session) {
		JsonObject mensaje = new JsonObject();
		mensaje.addProperty("action", ServerEvents.JUGADORES_ACTUALES);

		JsonArray jugadores = new JsonArray();
		for (Player player : players.values()) {
			JsonObject jugador = new JsonObject();
			jugador.addProperty("team", player.getTeam());
			jugadores.add(jugador);
		}
		mensaje.add("jugadores", jugadores);

		NotificationHelper.sendMessage(session, mensaje.toString());
	}

	public void handleDisconnection(Session session, Set<Session> sessions) {
		String playerId = session.getId();
		Player player = players.get(playerId);

		if (player != null) {
			String teamName = player.getTeam();
			players.remove(playerId);

			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.JUGADOR_DESCONECTADO);
			mensaje.addProperty("team", teamName);

			// Notificar a los demás jugadores
			for (Player otherPlayer : players.values()) {
				if (!otherPlayer.getSession().getId().equals(playerId)) {
					NotificationHelper.sendMessage(otherPlayer.getSession(), mensaje.toString());
				}
			}
			// Notificar a todas las sesiones activas
			for (Session activeSession : sessions) {
				if (!activeSession.getId().equals(playerId)) {
					NotificationHelper.sendMessage(activeSession, mensaje.toString());
				}
			}
		}
	}
}
