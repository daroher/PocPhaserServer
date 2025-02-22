package server;

public class GameEvent {
	private String action;
	private String team;
	private float x;
	private float y;
	private float visionRadius;
	private int angle;

	public String getAction() {
		return action;
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

	public int getAngle() {
		return angle;
	}

}