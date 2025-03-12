package vo;

public class GameEventFrance extends GameEvent {

	private float franceX;
	private float franceY;

	public GameEventFrance() {
		super();
	}

	public GameEventFrance(float franceX, float franceY) {
		super();
		this.franceX = franceX;
		this.franceY = franceY;
	}

	public void setFranceX(float franceX) {
		this.franceX = franceX;
	}

	public float getFranceX() {
		return franceX;
	}

	public void setFranceY(float franceY) {
		this.franceY = franceY;
	}

	public float getFranceY() {
		return franceY;
	}

}