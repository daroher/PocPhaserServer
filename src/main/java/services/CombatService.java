package services;

import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import logica.Plane;
import logica.Player;
import utils.NotificationHelper;
import utils.ServerEvents;
import vo.GameEvent;

public class CombatService {
	private static final Gson gson = new Gson();

	public CombatService() {
	}

	public void handleShoot(Session senderSession, String data, Set<Session> sessions, Map<String, Player> players) {
		try {
			GameEvent shootEvent = gson.fromJson(data, GameEvent.class);
			if ("bismarck".equals(shootEvent.getTeam())) {
				if (GameStateService.getInstance().getCantAviones() > 0) {
					GameStateService.getInstance().decrementAviones();
				}
			} else {
				if (GameStateService.getInstance().getVidaBismarck() > 0) {
					GameStateService.getInstance().decrementVidaBismarck();
				}
			}

			for (Player player : players.values()) {
				JsonObject message = new JsonObject();
				if ("bismarck".equals(shootEvent.getTeam())) {
					message.addProperty("action", ServerEvents.AVION_ELIMINADO);
					message.addProperty("cantAviones", GameStateService.getInstance().getCantAviones());
				} else {
					message.addProperty("action", ServerEvents.DISPARO_A_BISMARCK);
					message.addProperty("vidaBismarck", GameStateService.getInstance().getVidaBismarck());
				}
				NotificationHelper.sendMessage(player.getSession(), message.toString());
			}

			GameStateService.getInstance().checkVictory(sessions, players);
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handleBismarckBullet(Session senderSession, String data, Map<String, Player> players) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			float angle = playerEvent.getRelativeAngle();
			float vsRelativeDistanceX = playerEvent.getVsRelativeDistanceX();
			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.DISPARO_BALA_BISMARCK);
					positionMessage.addProperty("angle", angle);
					positionMessage.addProperty("vsRelativeDistanceX", vsRelativeDistanceX);
					NotificationHelper.sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handlePlaneBullet(Session senderSession, String data, Map<String, Player> players) {
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
					NotificationHelper.sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	public void handleNewPlane(Session senderSession, String data, Map<String, Player> players) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
			float x = playerEvent.getX();
			float y = playerEvent.getY();
			float angle = playerEvent.getAngle();
			boolean withPilot = playerEvent.isWithPilot();
			boolean withObserver = playerEvent.isWithObserver();
			boolean withOperator = playerEvent.isWithOperator();

			Player player = players.get(senderSession.getId());
			if (player != null) {
				// Actualizamos flag de observer en el jugador
				player.setWithObserver(withObserver);
				player.setWithOperator(withOperator);
				Plane plane = new Plane(x, y, angle, withPilot, withObserver, withOperator);

				JsonObject responseMessage = new JsonObject();
				responseMessage.addProperty("action", ServerEvents.DATOS_AVION);
				responseMessage.addProperty("x", plane.getPosX());
				responseMessage.addProperty("y", plane.getPosY());
				responseMessage.addProperty("visionRadius", plane.getVisionRadius());
				responseMessage.addProperty("speed", plane.getSpeed());
				responseMessage.addProperty("angle", plane.getAngle());
			    responseMessage.addProperty("fuelQty", plane.getFuelAmount());
	            responseMessage.addProperty("fuelConsumptionQty", plane.getFuelConsumptionRate());

				NotificationHelper.sendMessage(senderSession, responseMessage.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}
}
