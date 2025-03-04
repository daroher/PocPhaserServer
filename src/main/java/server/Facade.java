package server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import logica.Player;
import services.CombatService;
import services.ConnectionService;
import services.GameStateService;
import services.PlayerService;

public class Facade {
	// Mapa compartido de jugadores
	private Map<String, Player> players = new ConcurrentHashMap<>();

	private ConnectionService connectionHandler;
	private PlayerService playerHandler;
	private CombatService combatHandler;
	// GameStateService y VisionService son singleton

	public Facade() {
		connectionHandler = new ConnectionService(players);
		playerHandler = new PlayerService(players);
		combatHandler = new CombatService(players);
	}

	// Conexiones
	public void handleNewConnection(Session session) {
		connectionHandler.handleNewConnection(session);
	}

	public void handleDisconnection(Session session, Set<Session> sessions) {
		connectionHandler.handleDisconnection(session, sessions);
	}

	// Acciones de jugador
	public void handleNewPlayer(Session senderSession, String data, Set<Session> sessions) {
		playerHandler.handleNewPlayer(senderSession, data, sessions);
	}

	public void handleMovePlayer(Session senderSession, String data) {
		playerHandler.handleMovePlayer(senderSession, data);
	}

	public void handleMovePlayerWar(Session senderSession, String data) {
		playerHandler.handleMovePlayerWar(senderSession, data);
	}

	public void handleMovePlayerSideview(Session senderSession, String data) {
		playerHandler.handleMovePlayerSideview(senderSession, data);
	}

	public void handleEndSideview(Session senderSession, String data) {
		playerHandler.handleEndSideview(senderSession, data);
	}

	// Eventos de combate y balística
	public void handleShoot(Session senderSession, String data, Set<Session> sessions) {
		combatHandler.handleShoot(senderSession, data, sessions);
	}

	public void handleBismarckBullet(Session senderSession, String data) {
		combatHandler.handleBismarckBullet(senderSession, data);
	}

	public void handlePlaneBullet(Session senderSession, String data) {
		combatHandler.handlePlaneBullet(senderSession, data);
	}

	public void handleNewPlane(Session senderSession, String data) {
		combatHandler.handleNewPlane(senderSession, data);
	}

	// Eventos de estado de juego
	public void handleFindFrance(Session senderSession, String data) {
		GameStateService.getInstance().handleFindFrance(senderSession, data, players);
	}

	public void handleAircraftCarrierPositionSelection(Session senderSession, String data) {
		GameStateService.getInstance().handleAircraftCarrierPositionSelection(senderSession, data, players);
	}
}
