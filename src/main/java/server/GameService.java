package server;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import logica.Player;
import services.CombatService;
import services.ConnectionService;
import services.GameStateService;
import services.PlayerService;
import utils.NotificationHelper;
import utils.ServerEvents;
import vo.GameEvent;

public class GameService {
	private static final Gson gson = new Gson();
	private static GameService instance;

	// Estado compartido
	private final Map<String, Player> players = Collections.synchronizedMap(new HashMap<>());
	private final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
	private ConnectionService connectionService = new ConnectionService();
	private PlayerService playerService = new PlayerService();
	private GameStateService gameStateService = GameStateService.getInstance();
	private CombatService combatService = new CombatService();

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
		connectionService.handleNewConnection(session, players, sessions);
	}

	// Manejo de mensajes
	public void onMessage(String message, Session senderSession) {
		try {
			GameEvent playerEvent = gson.fromJson(message, GameEvent.class);
			if (playerEvent == null || playerEvent.getAction() == null) {
				System.err.println("Error: mensaje inválido." + message);
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
		case ServerEvents.JUGADORES_ACTUALES:
		    System.out.println("[Servidor] Enviando lista de jugadores actuales.");

		    JsonObject mensaje = new JsonObject();
		    mensaje.addProperty("action", ServerEvents.JUGADORES_ACTUALES);
		    
		    JsonArray jugadoresJson = new JsonArray();
		    for (Player p : players.values()) {
		        JsonObject jugador = new JsonObject();
		        jugador.addProperty("team", p.getTeam());
		        jugadoresJson.add(jugador);
		    }
		    mensaje.add("jugadores", jugadoresJson);

		 // Enviar la lista de jugadores a todos los clientes conectados
		    for (Session session : sessions) {
		        if (session.isOpen()) {
		            NotificationHelper.sendMessage(session, mensaje.toString());
		        }
		    }
		    
		    break;
		case ServerEvents.MUEVO_JUGADOR:
			playerService.handleMovePlayer(senderSession, data, players);
			break;
		case ServerEvents.LLEGA_FRANCIA:
			gameStateService.handleFindFrance(senderSession, data, players);
			break;
		case ServerEvents.DISPARO_ACERTADO:
			combatService.handleShoot(senderSession, data, sessions, players);
			break;
		case ServerEvents.MUEVO_JUGADOR_GUERRA:
			playerService.handleMovePlayerWar(senderSession, data, players);
			break;
		case ServerEvents.DISPARO_BALA_BISMARCK:
			combatService.handleBismarckBullet(senderSession, data, players);
			break;
		case ServerEvents.DISPARO_BALA_AVION:
			combatService.handlePlaneBullet(senderSession, data, players);
			break;
		case ServerEvents.NUEVO_AVION:
			combatService.handleNewPlane(senderSession, data, players);
			break;
		case ServerEvents.SELECCION_POSICION_PORTAAVIONES:
			gameStateService.handleAircraftCarrierPositionSelection(senderSession, data, players);
			break;
		case ServerEvents.MUEVO_JUGADOR_VENTAJA:
			playerService.handleMovePlayerSideview(senderSession, data, players);
			break;
		case ServerEvents.FINALIZA_VENTAJA:
			playerService.handleEndSideview(senderSession, data, players);
			break;
		case ServerEvents.CONSULTAR_POSICION_BISMARCK:
			playerService.handleConsultarPosicionBismarck(senderSession, data);
			break;
		case ServerEvents.AVION_SIN_COMBUSTIBLE:
			playerService.handleAvionSinCombustible(senderSession, data, players, sessions);
			break;
		case ServerEvents.SOLICITA_VOLVER_PORTAVIONES:
			playerService.handleVolverPortaviones(senderSession, data, players);
			break;
		case ServerEvents.PAUSAR_JUEGO:
			gameStateService.pauseGame(players);
			break;
		case ServerEvents.REANUDAR_JUEGO:
			gameStateService.resumeGame(players);
			break;
		case ServerEvents.SOLICITAR_GUARDAR_JUEGO:
			gameStateService.requestSaveGame(players);
			break;
		case ServerEvents.GUARDAR_JUEGO:
			gameStateService.saveGame(players, data);
			break;
		case ServerEvents.SOLICITAR_CARGAR_JUEGO:
			gameStateService.requestLoadGame(players);
			break;
		case ServerEvents.SELECCION_JUEGO:
		    gameStateService.handleGameSelection(senderSession, data, players, sessions);
		    break;
		case ServerEvents.CONFIRMAR_JUEGO:
		    gameStateService.confirmGameSelection(senderSession, data, players, sessions);
		    break;
		case ServerEvents.SALIR_JUEGO:
		    // Se reenvía a todas las sesiones
		    JsonObject exitMessage = new JsonObject();
		    exitMessage.addProperty("action", ServerEvents.SALIR_JUEGO);
		    for (Session session : sessions) {
		        if (session.isOpen()) {
		            NotificationHelper.sendMessage(session, exitMessage.toString());
		        }
		    }
		    break;

		default:
			System.err.println("Acción no reconocida: " + action);
			break;
		}
	}

}
