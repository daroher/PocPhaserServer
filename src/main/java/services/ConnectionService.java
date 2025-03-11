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

    public ConnectionService() {
    }

    public void handleNewConnection(Session session, Map<String, Player> players, Set<Session> sessions) {
        sessions.add(session);
        String playerId = session.getId();
        
        // Verificar si el jugador ya existe en el mapa (evitar duplicados)
        if (!players.containsKey(playerId)) {
            players.put(playerId, new Player()); // Puedes asignar un equipo después
        }

        // Crear mensaje de jugadores actuales
        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("action", ServerEvents.JUGADORES_ACTUALES);
        
        JsonArray jugadores = new JsonArray();
        for (Player player : players.values()) {
            JsonObject jugador = new JsonObject();
            jugador.addProperty("team", player.getTeam());
            jugadores.add(jugador);
        }
        mensaje.add("jugadores", jugadores);

        // Enviar el mensaje a TODOS los jugadores conectados
        for (Session activeSession : sessions) {
            if (activeSession.isOpen()) {
                NotificationHelper.sendMessage(activeSession, mensaje.toString());
            }
        }

        System.out.println("[Servidor] Jugadores conectados: " + players.size());
    }

    public void handleDisconnection(Session session, Set<Session> sessions, Map<String, Player> players) {
        // Remover la sesión desconectada
        sessions.remove(session);

        String playerId = session.getId();
        Player player = players.get(playerId);
        if (player != null) {
            players.remove(playerId);
        }

        JsonObject mensaje = new JsonObject();
        mensaje.addProperty("action", ServerEvents.JUGADORES_ACTUALES);
        
        JsonArray jugadoresJson = new JsonArray();
        for (Player p : players.values()) {
            JsonObject jugador = new JsonObject();
            jugador.addProperty("team", p.getTeam());
            jugadoresJson.add(jugador);
        }
        mensaje.add("jugadores", jugadoresJson);

        // Notificar a todas las sesiones activas
        for (Session activeSession : sessions) {
            if (activeSession.isOpen()) {
                NotificationHelper.sendMessage(activeSession, mensaje.toString());
            }
        }

        System.out.println("[Servidor] Un jugador se desconectó. Jugadores restantes: " + players.size());
    }
}
