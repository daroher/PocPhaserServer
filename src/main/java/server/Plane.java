package server;

public class Plane extends GameElement {

	private String estado; // ver de hacerlo enum?}
	private boolean withPilot;
	private boolean withObserver;
	private boolean withOperator;
	private int defaultSpeed;

	public Plane(float posX, float posY, float angle, boolean withPilot, boolean withObserver, boolean withOperator) {
		super(posX, posY, angle);
		this.estado = "Funcional";
		this.withPilot = withPilot;
		this.withObserver = withObserver;
		this.withOperator = withOperator;
		this.defaultSpeed = 3;
	}

	private int getTripulantes() {
		int count = 0;
		if (this.withPilot)
			count++;
		if (this.withObserver)
			count++;
		if (this.withOperator)
			count++;
		return count;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	@Override
	public float getSpeed() {
		if (this.getTripulantes() == 1) {
			return this.defaultSpeed;
		}

		double decrement = this.getTripulantes() * 0.20; // decremento

		if (decrement > 1.0) {
			decrement = 1.0;
		}

		return (float) (defaultSpeed * (1 - decrement));
	}

	@Override
	public int getVisionRadius() {
		return this.withObserver ? 500 : 300;
	}
}
