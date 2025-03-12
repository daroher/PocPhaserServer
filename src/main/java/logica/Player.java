package logica;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

public class Player {
	private String team;
	private float x;
	private float y;
	private float visionRadius;
	private float angle;
	private Session session;
	private Map<Player, Boolean> inVisionRangeOfPlayers = new HashMap<Player, Boolean>();
	private boolean withObserver;
	private boolean withOperator;
	private boolean isPlaneActive;

	public Player(String id, String team, float x, float y, float visionRadius, Session session, float angle) {
		this.team = team;
		this.x = x;
		this.y = y;
		this.visionRadius = visionRadius;
		this.session = session;
		this.angle = angle;
		this.withObserver = withObserver;
		this.withOperator = withOperator;
	}

	public String getTeam() {
		return team;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getVisionRadius() {
		return visionRadius;
	}

	public Session getSession() {
		return session;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setVisionRadius(float visionRadius) {
		this.visionRadius = visionRadius;
	}

	// Verifica si este jugador está dentro del rango de visión de otro jugador
	public boolean isInVisionRangeOf(Player otherPlayer) {
		return this.inVisionRangeOfPlayers.getOrDefault(otherPlayer, false);
	}

	public void setInVisionRangeOf(Player otherPlayer, boolean inRange) {
		this.inVisionRangeOfPlayers.put(otherPlayer, inRange);
	}

	public float getAngle() {
		return this.angle;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public boolean isWithObserver() {
		return withObserver;
	}

	public void setWithObserver(boolean withObserver) {
		this.withObserver = withObserver;
	}

	public boolean isWithOperator() {
		return withOperator;
	}

	public void setWithOperator(boolean withOperator) {
		this.withOperator = withOperator;
	}
	
	public boolean isPlaneActive() {
		return this.isPlaneActive;
	}

	public void setIsPlaneActive(boolean isPlaneActive) {
		this.isPlaneActive = isPlaneActive;
	}

}
