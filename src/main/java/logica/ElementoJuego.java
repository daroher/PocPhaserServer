package logica;

public abstract class ElementoJuego {
	private float posX;
	private float posY;
	private float angle;

	public ElementoJuego() {
		super();
	}

	public ElementoJuego(float posX, float posY, float angle) {
		this.posX = posX;
		this.posY = posY;
		this.angle = angle;
	}

	public float getPosX() {
		return posX;
	}

	public void setPosX(float posX) {
		this.posX = posX;
	}

	public float getPosY() {
		return posY;
	}

	public void setPosY(float posY) {
		this.posY = posY;
	}

	public float getAngle() {
		return angle;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public abstract float getSpeed();

	public abstract float getVisionRadius();

}