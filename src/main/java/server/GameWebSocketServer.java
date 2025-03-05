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

import logica.GameEvent;
import logica.GameEventFrance;
import logica.Plane;
import logica.Player;
import utils.ServerEvents;

@ServerEndpoint("/game2")
public class GameWebSocketServer {
	private static final Gson gson = new Gson();
	private static final Map<String, Player> players = Collections.synchronizedMap(new HashMap<>());
	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
	private int cantAviones = 10;
	private int vidaBismarck = 3;

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
		case ServerEvents.NUEVO_AVION:
			handleNewPlane(senderSession, data);
			break;
		case ServerEvents.SELECCION_POSICION_PORTAAVIONES:
			handleAircraftCarrierPositionSelection(senderSession, data);
		case ServerEvents.MUEVO_JUGADOR_VENTAJA:
			handleMovePlayerSideview(senderSession, data);
			break;
		case ServerEvents.FINALIZA_VENTAJA:
			handleEndSideview(senderSession, data);
			break;
		default:
			System.err.println("Acción no reconocida: " + action);
			break;
		}
	}

	private void handleNewPlayer(Session senderSession, String data) {
		
		GameEvent playerEvent = gson.fromJson(data, GameEvent.class);
		
		System.out.println("selecciono:" + playerEvent.isWithObserver());
		
		Player player = new Player(senderSession.getId(), playerEvent.getTeam(), playerEvent.getX(), playerEvent.getY(),
				playerEvent.getVisionRadius(), senderSession, playerEvent.getAngle());
		players.put(senderSession.getId(), player);

		//TODO: parametrizar los teams
		if ("bismarck".equals(player.getTeam())) {
			this.vidaBismarck = 3;
		} else {
			this.cantAviones = 10;
		}

		for (Session session : sessions) {
			if (!session.getId().equals(senderSession.getId())) {
				sendMessage(session, data);
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
			messageInRange.addProperty("distance", Math
					.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2)));

			sendMessage(observer.getSession(), messageInRange.toString());

			System.out.println("player:" + observer.getTeam() + "- observer:" + target.isWithObserver());
			//Si el bismrack lo vio, no esta en enfriamiento de ventaja y el avion no tenia observador, entonces es ventaja para bismarck
			//TODO:implementar cooldown de ventaja
			if(observer.getTeam().equals("bismarck") && !target.isWithObserver()) {
				
				// Mensaje de ventaja, envio la informacion de sus posiciones para recomponer la vista desde arriba cuando termine la ventaja
				JsonObject MessageVentaja = new JsonObject();
				MessageVentaja.addProperty("action", ServerEvents.INICIA_VENTAJA);
				MessageVentaja.addProperty("startTeam", observer.getTeam());
				MessageVentaja.addProperty("otherTeam", target.getTeam());
				MessageVentaja.addProperty("distance", Math
						.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2)));
				
				sendMessage(observer.getSession(), MessageVentaja.toString());
				sendMessage(target.getSession(), MessageVentaja.toString());
			}else if ((observer.getTeam().equals("britanicos") && observer.isWithObserver())
					|| (observer.getTeam().equals("bismarck") && target.isWithObserver())){			
				
				System.out.println("Guerra");
				// Mensaje de guerra
				JsonObject guerraMessage = new JsonObject();
				guerraMessage.addProperty("action", ServerEvents.INICIA_GUERRA);
				guerraMessage.addProperty("startTeam", observer.getTeam());
				guerraMessage.addProperty("otherTeam", target.getTeam());
				guerraMessage.addProperty("distance", Math
						.sqrt(Math.pow(observer.getX() - target.getX(), 2) + Math.pow(observer.getY() - target.getY(), 2)));
	
				sendMessage(observer.getSession(), guerraMessage.toString());
				sendMessage(target.getSession(), guerraMessage.toString());
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

			sendMessage(observer.getSession(), message.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendMessage(Session session, String message) {
		synchronized (session) {
			try {
				if (session.isOpen()) {
					System.out.println("send message: " + message);
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
				
				sendMessage(player.getSession(), message.toString());
			}
			
			checkVictory();
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
			float vsDistanceZ = playerEvent.getVsDistanceZ();

			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {

					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.MUEVO_JUGADOR_GUERRA);
					positionMessage.addProperty("team", team);
					positionMessage.addProperty("x", x);
					positionMessage.addProperty("y", y);
					positionMessage.addProperty("vsDistanceZ", vsDistanceZ);

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
			float vsRelativeDistanceX = playerEvent.getVsRelativeDistanceX();
			
			for (Player player : players.values()) {
				if (!player.getSession().getId().equals(senderSession.getId())) {
					JsonObject positionMessage = new JsonObject();
					positionMessage.addProperty("action", ServerEvents.DISPARO_BALA_BISMARCK);
					positionMessage.addProperty("angle", angle);
					positionMessage.addProperty("vsRelativeDistanceX", vsRelativeDistanceX);
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

				//actualizo si el player tiene observer
				player.setWithObserver(withObserver);
				players.put(senderSession.getId(), player);
				
				Plane plane = new Plane(x, y, angle, withPilot, withObserver, withOperator);
				// Ver como manejar la collecion de aviones

				JsonObject responseMessage = new JsonObject();
				responseMessage.addProperty("action", ServerEvents.DATOS_AVION);
				responseMessage.addProperty("x", plane.getPosX());
				responseMessage.addProperty("y", plane.getPosY());
				responseMessage.addProperty("visionRadius", plane.getVisionRadius());
				responseMessage.addProperty("speed", plane.getSpeed());
				responseMessage.addProperty("angle", plane.getAngle());

				sendMessage(senderSession, responseMessage.toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
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
							sendMessage(otherPlayer.getSession(), mensaje.toString());
						}
					}					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
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
				sendMessage(player.getSession(), victoryMessage.toString());
			}
		}

		players.clear();
		sessions.clear();
	}

	private void handleMovePlayerSideview(Session senderSession, String data) {
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

					sendMessage(player.getSession(), positionMessage.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(senderSession, "Error: " + e.getMessage());
		}
	}


	private void handleEndSideview(Session senderSession, String data) {
		for (Player player : players.values()) {
			JsonObject mensaje = new JsonObject();
			mensaje.addProperty("action", ServerEvents.VOLVER_VISTA_SUPERIOR);
			mensaje.addProperty("x", player.getX());
			mensaje.addProperty("y", player.getY());
			sendMessage(player.getSession(), mensaje.toString());
		}
		
	}
}
