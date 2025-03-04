package services;

import java.util.Map;

import com.google.gson.JsonObject;

import logica.Player;
import utils.NotificationHelper;
import utils.ServerEvents;

public class VisionService {
	private static VisionService instance;

	private VisionService() {
	}

	public static VisionService getInstance() {
		if (instance == null) {
			instance = new VisionService();
		}
		return instance;
	}

	public void checkMapVision(Player player, Map<String, Player> players) {
		for (Player otherPlayer : players.values()) {
			if (otherPlayer.getSession().getId().equals(player.getSession().getId()))
				continue;

			float distance = (float) Math.sqrt(
					Math.pow(player.getX() - otherPlayer.getX(), 2) + Math.pow(player.getY() - otherPlayer.getY(), 2));

			// Verificar si el jugador actual está dentro del rango del otro jugador
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

			// Verificar si el otro jugador está dentro del rango del jugador actual
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
			JsonObject messageInRange = new JsonObject();
			messageInRange.addProperty("action", ServerEvents.JUGADOR_EN_RANGO);
			messageInRange.addProperty("x", target.getX());
			messageInRange.addProperty("y", target.getY());
			messageInRange.addProperty("team", target.getTeam());
			messageInRange.addProperty("angle", target.getAngle());
			float distance = (float) Math
					.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2));
			messageInRange.addProperty("distance", distance);

			NotificationHelper.sendMessage(observer.getSession(), messageInRange.toString());

			// Notificar ventaja o guerra según reglas de negocio
			if (observer.getTeam().equals("bismarck") && !target.isWithObserver()) {
				JsonObject messageVentaja = new JsonObject();
				messageVentaja.addProperty("action", ServerEvents.INICIA_VENTAJA);
				messageVentaja.addProperty("startTeam", observer.getTeam());
				messageVentaja.addProperty("otherTeam", target.getTeam());
				messageVentaja.addProperty("distance", distance);
				NotificationHelper.sendMessage(observer.getSession(), messageVentaja.toString());
				NotificationHelper.sendMessage(target.getSession(), messageVentaja.toString());
			} else if ((observer.getTeam().equals("britanicos") && observer.isWithObserver())
					|| (observer.getTeam().equals("bismarck") && target.isWithObserver())) {

				System.out.println("Guerra");
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
}
