package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import logica.AvionBritanico;
import logica.Bismarck;
import logica.Francia;
import logica.Jugador;
import logica.Partida;
import logica.Player;
import logica.Portavion;
import logica.Tripulante;
import utils.NotificationHelper;
import utils.ServerEvents;
import vo.GameEvent;
import vo.GameEventFrance;

public class GameStateService {
	private int cantAviones = 10;
	private int vidaBismarck = 3;

	private static GameStateService instance;

	private final Gson gson = new Gson();
	private final Map<String, PartidaData> partialSaves = new ConcurrentHashMap<>();

	// Clase para almacenar datos parciales de la partida
	private static class PartidaData {
		PlayerData bismarckData;
		PlayerData britanicosData;
	}

	// Clase para mapear datos comunes del JSON
	private static class PlayerData {
		float x;
		float y;
		float angle;
		float visionRadius;
		String team;
		BismarckData bismarck; // Solo para equipo Bismarck
		PortavionData portavion; // Solo para equipo Britanicos
		AvionActivoData avionActivo; // Solo para equipo Britanicos
	}

	// Clases para datos específicos de cada equipo
	private static class BismarckData {
		float franceX;
		float franceY;
	}

	private static class PortavionData {
		float posX;
		float posY;
		float angle;
		int avionesDisponibles;
	}

	private static class AvionActivoData {
		String estado;
		List<String> tripulantes;
		boolean withPilot;
		boolean withObserver;
		boolean withOperator;
		float fuelAmount;
	}

	private Partida crearPartidaCompleta(PartidaData data) {
		Partida partida = new Partida();
		partida.setId(UUID.randomUUID().toString());
		partida.setEstado("GUARDADA");

		// Crear jugador Bismarck
		Jugador jugadorBismarck = new Jugador();
		Bismarck bismarck = new Bismarck();
		bismarck.setPosX(data.bismarckData.x);
		bismarck.setPosY(data.bismarckData.y);
		bismarck.setAngle(data.bismarckData.angle);
		jugadorBismarck.setElementoJuego(bismarck);
		jugadorBismarck.setEquipo("BISMARCK");

		// Crear evento Francia
		Francia francia = new Francia();
		francia.setPosX(data.bismarckData.bismarck.franceX);
		francia.setPosY(data.bismarckData.bismarck.franceY);

		// Crear jugador Británicos
		Jugador jugadorBritanicos = new Jugador();
		if (data.britanicosData.avionActivo != null && "Funcional".equals(data.britanicosData.avionActivo.estado)) {
			AvionBritanico avion = new AvionBritanico(data.britanicosData.x, data.britanicosData.y,
					data.britanicosData.angle, data.britanicosData.avionActivo.estado);
			avion.setTripulantes(convertirTripulantes(data.britanicosData.avionActivo.tripulantes));
			jugadorBritanicos.setElementoJuego(avion);
		} else {
			Portavion portavion = new Portavion(data.britanicosData.portavion.posX, data.britanicosData.portavion.posY,
					data.britanicosData.portavion.angle);
			portavion.setCantAviones(data.britanicosData.portavion.avionesDisponibles);
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
	    return tripulantesStr.stream()
	            .map(Tripulante::fromTipo) // Usa el nuevo método
	            .collect(Collectors.toList());
	}

	private void guardarPartidaEnBD(Connection con, Partida partida) {
	    String insertPartida = "INSERT INTO BistmarckDB.Partidas (id, estado, jugadores, francia_x, francia_y, francia_angle) VALUES (?, ?, ?, ?, ?, ?)";
	    PreparedStatement pstmt = null;

	    try {
	        pstmt = con.prepareStatement(insertPartida);
	        pstmt.setString(1, partida.getId());
	        pstmt.setString(2, partida.getEstado());

	        String jugadoresJson = new Gson().toJson(partida.getJugadores());
	        pstmt.setString(3, jugadoresJson);

	        Francia francia = partida.getFrancia();
	        pstmt.setFloat(4, francia.getPosX());
	        pstmt.setFloat(5, francia.getPosY());
	        pstmt.setFloat(6, francia.getAngle());

	        pstmt.executeUpdate();
	        System.out.println("Partida guardada en la base de datos con éxito.");
	    } catch (SQLException e) {
	        System.out.println("ERROR: No se pudo guardar la partida.");
	        e.printStackTrace();
	    } finally {
	        try {
	            if (pstmt != null) pstmt.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	}

	private String obtenerIdPartidaActual() {
		// Implementar lógica para obtener ID de partida actual
		return "partida-unica"; // Ejemplo simplificado
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

	        // Bloque sincronizado para operaciones compuestas
	        synchronized (partialSaves) {
	            PartidaData partidaData = partialSaves.computeIfAbsent(partidaId, k -> new PartidaData());

	            if ("bismarck".equalsIgnoreCase(playerData.team)) {
	                partidaData.bismarckData = playerData;
	            } else if ("britanicos".equalsIgnoreCase(playerData.team)) {
	                partidaData.britanicosData = playerData;
	            }

	            if (partidaData.bismarckData != null && partidaData.britanicosData != null) {
	                Partida partidaCompleta = crearPartidaCompleta(partidaData);
	                
	            	String driver = "com.mysql.jdbc.Driver";
	            	Class.forName(driver);
	    			String url = "jdbc:mysql://localhost:3306/BistmarckDB";
	    			Connection con = DriverManager.getConnection(url, "root", "root");
	    			
	    			
	                guardarPartidaEnBD(con, partidaCompleta);
	                partialSaves.remove(partidaId); // Eliminar dentro del bloque sincronizado
	            }
	        }

	    } catch (JsonSyntaxException e) {
	        System.err.println("Error procesando JSON: " + e.getMessage());
	    } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void requestLoadGame(Map<String, Player> players) {

		JsonObject loadMessage = new JsonObject();
		loadMessage.addProperty("action", ServerEvents.CARGAR_JUEGO);
		
	        try { 
	        	
	        	String driver = "com.mysql.jdbc.Driver";
            	Class.forName(driver);
    			String url = "jdbc:mysql://localhost:3306/BistmarckDB";
    			Connection con = DriverManager.getConnection(url, "root", "root");
	        	
	            // En este ejemplo, obtenemos la última partida guardada
	            String sql = "SELECT id, estado, jugadores, francia_x, francia_y, francia_angle FROM BistmarckDB.Partidas ORDER BY id DESC LIMIT 1";
	            try (PreparedStatement ps = con.prepareStatement(sql);
	                 ResultSet rs = ps.executeQuery()) {

	                if (rs.next()) {
	                    // Construimos el objeto JSON con los datos obtenidos
	                    loadMessage.addProperty("id", rs.getString("id"));
	                    loadMessage.addProperty("estado", rs.getString("estado"));
	                    loadMessage.addProperty("jugadores", rs.getString("jugadores"));
	                    
	                    JsonObject francia = new JsonObject();
	                    francia.addProperty("x", rs.getFloat("francia_x"));
	                    francia.addProperty("y", rs.getFloat("francia_y"));
	                    francia.addProperty("angle", rs.getFloat("francia_angle"));
	                    
	                    loadMessage.add("francia", francia);
	                    
	                    // Creamos el mensaje final con el evento a enviar
	                    JsonObject message = new JsonObject();
	                    message.addProperty("event", "CARGAR_JUEGO");
	                    message.add("data", loadMessage);
	                    
	                } else {
	                    System.out.println("No se encontró partida guardada.");
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        } catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    

		for (Player player : players.values()) {
			if (player.getSession().isOpen()) {
				System.out.println("mensaje:" + loadMessage.toString());
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
