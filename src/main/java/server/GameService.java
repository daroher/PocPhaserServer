package server;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import logica.GameEvent;
import logica.GameEventFrance;
import logica.Plane;
import logica.Player;
import services.ConnectionService;
import services.PlayerService;
import utils.NotificationHelper;
import utils.ServerEvents;

public class GameService {
	private static final Gson gson = new Gson();
	private static GameService instance;

	// Estado compartido
	private final Map<String, Player> players = Collections.synchronizedMap(new HashMap<>());
	private final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
	private ConnectionService connectionService = new ConnectionService();
	private PlayerService playerService = new PlayerService();
	private int cantAviones = 10;
	private int vidaBismarck = 3;

	private GameService() {
	}

	public static synchronized GameService getInstance() {
		if (instance == null) {
			instance = new GameService();
		}
		return instance;
	}

	// Manejo de conexión
	public void onOpen(Session session) {
		connectionService.handleNewConnection(session, players);
	}

	// Manejo de mensajes
	public void onMessage(String message, Session senderSession) {
		try {
			GameEvent playerEvent = gson.fromJson(message, GameEvent.class);
			if (playerEvent == null || playerEvent.getAction() == null) {
				System.err.println("Error: mensaje inválido.");
				return;
			}
			messageReducer(playerEvent.getAction(), senderSession, message);
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	// Manejo de desconexión
	public void onClose(Session session) {
		connectionService.handleDisconnection(session, sessions, players);
	}

	// Despachador de acciones
	private void messageReducer(String action, Session senderSession, String data) {
		switch (action) {
		case ServerEvents.NUEVO_JUGADOR:
			playerService.handleNewPlayer(senderSession, data, sessions, players);
			break;
		case ServerEvents.MUEVO_JUGADOR:
			playerService.handleMovePlayer(senderSession, data, players);
			break;
		case ServerEvents.LLEGA_FRANCIA:
			handleFindFrance(senderSession, data);
			break;
		case ServerEvents.DISPARO_ACERTADO:
			handleShoot(senderSession, data);
			break;
		case ServerEvents.MUEVO_JUGADOR_GUERRA:
			playerService.handleMovePlayerWar(senderSession, data, players);
			break;
		case ServerEvents.DISPARO_BALA_BISMARCK:
			handleBismarckBullet(senderSession, data);
			break;
		case ServerEvents.DISPARO_BALA_AVION:
			handlePlaneBullet(senderSession, data);
			break;
		case ServerEvents.NUEVO_AVION:
			handleNewPlane(senderSession, data);
			break;
		case ServerEvents.SELECCION_POSICION_PORTAAVIONES:
			handleAircraftCarrierPositionSelection(senderSession, data);
			break;
		case ServerEvents.MUEVO_JUGADOR_VENTAJA:
			playerService.handleMovePlayerSideview(senderSession, data, players);
			break;
		case ServerEvents.FINALIZA_VENTAJA:
			playerService.handleEndSideview(senderSession, data, players);
			break;
		default:
			System.err.println("Acción no reconocida: " + action);
			break;
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
						NotificationHelper.sendMessage(p.getSession(), message.toString());
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
			if ("bismarck".equals(shootEvent.getTeam())) {
				if (this.cantAviones > 0) {
					this.cantAviones--;
				}
			} else {
				if (this.vidaBismarck > 0) {
					this.vidaBismarck--;
				}
			}
			for (Player player : players.values()) {
				JsonObject message = new JsonObject();
				if ("bismarck".equals(shootEvent.getTeam())) {
					message.addProperty("action", ServerEvents.AVION_ELIMINADO);
					message.addProperty("cantAviones", this.cantAviones);
				} else {
					message.addProperty("action", ServerEvents.DISPARO_A_BISMARCK);
					message.addProperty("vidaBismarck", this.vidaBismarck);
				}
				NotificationHelper.sendMessage(player.getSession(), message.toString());
			}
			checkVictory();
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	private void handleBismarckBullet(Session senderSession, String data) {
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
					NotificationHelper.sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	private void handleNewPlane(Session senderSession, String data) {
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
				// Actualizar flag de observer
				player.setWithObserver(withObserver);
				players.put(senderSession.getId(), player);
				Plane plane = new Plane(x, y, angle, withPilot, withObserver, withOperator);
				JsonObject responseMessage = new JsonObject();
				responseMessage.addProperty("action", ServerEvents.DATOS_AVION);
				responseMessage.addProperty("x", plane.getPosX());
				responseMessage.addProperty("y", plane.getPosY());
				responseMessage.addProperty("visionRadius", plane.getVisionRadius());
				responseMessage.addProperty("speed", plane.getSpeed());
				responseMessage.addProperty("angle", plane.getAngle());
				NotificationHelper.sendMessage(senderSession, responseMessage.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			NotificationHelper.sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}

	private void handleAircraftCarrierPositionSelection(Session senderSession, String data) {
		try {
			GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
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

	private void checkVictory() {
		if (this.cantAviones == 0 || this.vidaBismarck == 0) {
			String team = (this.cantAviones == 0) ? "bismarck" : "britanicos";
			sendVictoryMessage(team);
		}
	}

	private void sendVictoryMessage(String team) {
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
