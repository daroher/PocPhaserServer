package services;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import dao.DAOPartida;
import dao.IDAOPartida;
import logica.AvionBritanico;
import logica.Bismarck;
import logica.Francia;
import logica.Jugador;
import logica.Partida;
import logica.Player;
import logica.Portavion;
import logica.Tripulante;
import utils.Conexion;
import utils.NotificationHelper;
import utils.ServerEvents;
import vo.GameEvent;
import vo.GameEventFrance;
import vo.PartidaData;
import vo.PlayerData;
import vo.PortavionData;

public class GameStateService {
	private int cantAviones = 10;
	private int vidaBismarck = 3;

	private static GameStateService instance;

	private final Gson gson = new Gson();
	private final Map<String, PartidaData> partialSaves = new ConcurrentHashMap<>();
	private IDAOPartida daoPartida = new DAOPartida();

	private Partida crearPartidaCompleta(PartidaData data) {
		Partida partida = new Partida();
		partida.setId(UUID.randomUUID().toString());
		partida.setEstado("GUARDADA");

		// Crear jugador Bismarck
		Jugador jugadorBismarck = new Jugador();
		Bismarck bismarck = new Bismarck();
		bismarck.setPosX(data.getBismarckData().getX());
		bismarck.setPosY(data.getBismarckData().getY());
		bismarck.setAngle(data.getBismarckData().getAngle());
		jugadorBismarck.setElementoJuego(bismarck);
		jugadorBismarck.setEquipo("BISMARCK");

		// Crear evento Francia
		Francia francia = new Francia();
		francia.setPosX(data.getBismarckData().getBismarck().getFranceX());
		francia.setPosY(data.getBismarckData().getBismarck().getFranceY());

		// Crear jugador Británicos
		Jugador jugadorBritanicos = new Jugador();
		if (data.getBritanicosData().getAvionActivo() != null
				&& "Funcional".equals(data.getBritanicosData().getAvionActivo().getEstado())) {
			AvionBritanico avion = new AvionBritanico(data.getBritanicosData().getX(), data.getBritanicosData().getY(),
					data.getBritanicosData().getAngle(), data.getBritanicosData().getAvionActivo().getEstado());
			avion.setTripulantes(convertirTripulantes(data.getBritanicosData().getAvionActivo().getTripulantes()));
			jugadorBritanicos.setElementoJuego(avion);
		} else {
			Portavion portavion = new Portavion(data.getBritanicosData().getPortavion().getPosX(),
					data.getBritanicosData().getPortavion().getPosY(),
					data.getBritanicosData().getPortavion().getAngle());
			portavion.setCantAviones(data.getBritanicosData().getPortavion().getAvionesDisponibles());
			jugadorBritanicos.setElementoJuego(portavion);
		}
		jugadorBritanicos.setEquipo("BRITANICOS");

		List<Jugador> jugadores = new ArrayList<>();
		jugadores.add(jugadorBismarck);
		jugadores.add(jugadorBritanicos);

		// Agregar componentes a la partida
		partida.setJugadores(jugadores);
		partida.setFrancia(francia);

		return partida;
	}

	private List<Tripulante> convertirTripulantes(List<String> tripulantesStr) {
		return tripulantesStr.stream().map(Tripulante::fromTipo) // Usa el nuevo método
				.collect(Collectors.toList());
	}

	private String obtenerIdPartidaActual() {
		return "partida-unica";
	}

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

	public void pauseGame(Map<String, Player> players) {

		JsonObject pauseMessage = new JsonObject();
		pauseMessage.addProperty("action", ServerEvents.PAUSAR_JUEGO);

		for (Player player : players.values()) {
			if (player.getSession().isOpen()) {
				NotificationHelper.sendMessage(player.getSession(), pauseMessage.toString());
			}
		}
	}

	public void resumeGame(Map<String, Player> players) {

		JsonObject resumeMessage = new JsonObject();
		resumeMessage.addProperty("action", ServerEvents.REANUDAR_JUEGO);

		for (Player player : players.values()) {
			if (player.getSession().isOpen()) {
				NotificationHelper.sendMessage(player.getSession(), resumeMessage.toString());
			}
		}
	}

	public void requestSaveGame(Map<String, Player> players) {

		JsonObject saveMessage = new JsonObject();
		saveMessage.addProperty("action", ServerEvents.SOLICITAR_GUARDAR_JUEGO);

		for (Player player : players.values()) {
			if (player.getSession().isOpen()) {
				NotificationHelper.sendMessage(player.getSession(), saveMessage.toString());
			}
		}
	}

	public void saveGame(Map<String, Player> players, String data) {
		try {
			PlayerData playerData = gson.fromJson(data, PlayerData.class);
			String partidaId = obtenerIdPartidaActual();

			synchronized (partialSaves) {
				PartidaData partidaData = partialSaves.computeIfAbsent(partidaId, k -> new PartidaData());

				if ("bismarck".equalsIgnoreCase(playerData.getTeam())) {
					partidaData.setBismarckData(playerData);
				} else if ("britanicos".equalsIgnoreCase(playerData.getTeam())) {
					if (partidaData.getBritanicosData() == null) {
						partidaData.setBritanicosData(new PlayerData());
					}

					// Si el avión está activo, guardar su información y sus coordenadas
					if (playerData.getAvionActivo() != null) {
						partidaData.getBritanicosData().setAvionActivo(playerData.getAvionActivo());
						partidaData.getBritanicosData().setX(playerData.getX());
						partidaData.getBritanicosData().setY(playerData.getY());
						partidaData.getBritanicosData().setAngle(playerData.getAngle());
					}

					// Guardar siempre la información del portaviones (sus coordenadas y demás)
					if (playerData.getPortavion() != null) {
						PortavionData portavionData = playerData.getPortavion();
						if (partidaData.getBritanicosData().getPortavion() == null) {
							partidaData.getBritanicosData().setPortavion(new PortavionData());
						}
						partidaData.getBritanicosData().getPortavion().setPosX(portavionData.getPosX());
						partidaData.getBritanicosData().getPortavion().setPosY(portavionData.getPosY());
						partidaData.getBritanicosData().getPortavion().setAngle(portavionData.getAngle());
						partidaData.getBritanicosData().getPortavion()
								.setAvionesDisponibles(portavionData.getAvionesDisponibles());
					}
				}

				if (partidaData.getBismarckData() != null && partidaData.getBritanicosData() != null) {
					Partida partidaCompleta = crearPartidaCompleta(partidaData);

					Connection con = new Conexion().obtenerConexion();
					daoPartida.guardarPartidaEnBD(con, partidaCompleta);

					partialSaves.remove(partidaId);
				}
			}

			// Enviar confirmación al cliente
			JsonObject successMessage = new JsonObject();
			successMessage.addProperty("action", ServerEvents.GUARDAR_JUEGO);
			successMessage.addProperty("status", "success");
			successMessage.addProperty("message", "Partida guardada correctamente");

			for (Player player : players.values()) {
				if (player.getSession().isOpen()) {
					NotificationHelper.sendMessage(player.getSession(), successMessage.toString());
				}
			}

		} catch (JsonSyntaxException e) {
			System.err.println("Error procesando JSON: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Error al guardar la partida: " + e.getMessage());
		}
	}

	public void requestLoadGame(Map<String, Player> players) {
		JsonObject loadMessage = new JsonObject();
		loadMessage.addProperty("action", ServerEvents.CARGAR_JUEGO);

		Connection con = new Conexion().obtenerConexion();
		JsonObject partidaJson = new JsonObject();
		daoPartida.obtenerPartida(con, partidaJson);

		// ✅ Convertir correctamente "jugadores" de String JSON a JsonArray
		JsonArray jugadoresJson;
		try {
			String jugadoresString = partidaJson.get("jugadores").getAsString();
			jugadoresJson = new Gson().fromJson(jugadoresString, JsonArray.class);
		} catch (JsonSyntaxException e) {
			System.err.println("Error al convertir jugadores a JsonArray: " + e.getMessage());
			return;
		}

		loadMessage.add("jugadores", jugadoresJson);

		// ✅ Enviar los datos a todos los jugadores conectados
		for (Player player : players.values()) {
			if (player.getSession().isOpen()) {
				NotificationHelper.sendMessage(player.getSession(), loadMessage.toString());
			}
		}
	}

	public void handleGameSelection(Session senderSession, String data, Map<String, Player> players,
			Set<Session> sessions) {
		JsonObject playerSelection = new Gson().fromJson(data, JsonObject.class);
		String selectedOption = playerSelection.get("option").getAsString();

		JsonObject selectionMessage = new JsonObject();
		selectionMessage.addProperty("action", ServerEvents.SELECCION_JUEGO);
		selectionMessage.addProperty("option", selectedOption);

		for (Session session : sessions) {
			if (session.isOpen() && !session.equals(senderSession)) {
				NotificationHelper.sendMessage(session, selectionMessage.toString());
			}
		}
	}

	public void confirmGameSelection(Session senderSession, String data, Map<String, Player> players,
			Set<Session> sessions) {
		JsonObject confirmSelection = new Gson().fromJson(data, JsonObject.class);
		String finalOption = confirmSelection.get("option").getAsString();

		JsonObject startGameMessage = new JsonObject();
		startGameMessage.addProperty("action", ServerEvents.CONFIRMAR_JUEGO);
		startGameMessage.addProperty("option", finalOption);

		for (Session session : sessions) {
			if (session.isOpen()) {
				NotificationHelper.sendMessage(session, startGameMessage.toString());
			}
		}
	}

}
