package server;

public class GameEventFrance extends GameEvent {

	private float franceX;
	private float franceY;

	public GameEventFrance(float franceX, float franceY) {
		super();
		this.franceX = franceX;
		this.franceY = franceY;
	}

	public float getFranceX() {
		return franceX;
	}

	public float getFranceY() {
		return franceY;
	}

}