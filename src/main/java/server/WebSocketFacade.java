package server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import logica.GameEvent;
import utils.ServerEvents;

@ServerEndpoint("/game")
public class WebSocketFacade {
	private static final Gson gson = new Gson();
	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
	private Facade fachada;

	public WebSocketFacade() {
		this.fachada = new Facade();
	}

	@OnOpen
	public void onOpen(Session session) {
		sessions.add(session);
		fachada.handleNewConnection(session);
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			handleMessage(session, message);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error: " + e.getMessage());
		}
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);
		fachada.handleDisconnection(session, sessions);
	}

	/**
	 * Procesa un mensaje recibido.
	 *
	 */
	private void handleMessage(Session session, String message) {
		GameEvent event = gson.fromJson(message, GameEvent.class);

		if (event == null || event.getAction() == null) {
			System.err.println("Error: Mensaje inválido.");
			return;
		}

		switch (event.getAction()) {
		case ServerEvents.NUEVO_JUGADOR:
			fachada.handleNewPlayer(session, message, sessions);
			break;
		case ServerEvents.MUEVO_JUGADOR:
			fachada.handleMovePlayer(session, message);
			break;
		case ServerEvents.LLEGA_FRANCIA:
			fachada.handleFindFrance(session, message);
			break;
		case ServerEvents.DISPARO_ACERTADO:
			fachada.handleShoot(session, message, sessions);
			break;
		case ServerEvents.MUEVO_JUGADOR_GUERRA:
			fachada.handleMovePlayerWar(session, message);
			break;
		case ServerEvents.DISPARO_BALA_BISMARCK:
			fachada.handleBismarckBullet(session, message);
			break;
		case ServerEvents.DISPARO_BALA_AVION:
			fachada.handlePlaneBullet(session, message);
			break;
		case ServerEvents.NUEVO_AVION:
			fachada.handleNewPlane(session, message);
			break;
		case ServerEvents.SELECCION_POSICION_PORTAAVIONES:
			fachada.handleAircraftCarrierPositionSelection(session, message);
		case ServerEvents.MUEVO_JUGADOR_VENTAJA:
			fachada.handleMovePlayerSideview(session, message);
			break;
		case ServerEvents.FINALIZA_VENTAJA:
			fachada.handleEndSideview(session, message);
			break;
		default:
			System.err.println("Acción no reconocida: " + event.getAction());
			break;
		}
	}
}
