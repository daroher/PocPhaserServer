package logica;

public class Plane extends GameElement {
	private String estado;
	private boolean withPilot;
	private boolean withObserver;
	private boolean withOperator;
	private int defaultSpeed;

	private float fuelAmount;
	private float fuelConsumptionRate;

	public Plane(float posX, float posY, float angle, boolean withPilot, boolean withObserver, boolean withOperator) {
		super(posX, posY, angle);
		this.estado = "Funcional";
		this.withPilot = withPilot;
		this.withObserver = withObserver;
		this.withOperator = withOperator;
		this.defaultSpeed = 3;

		this.fuelAmount = (float) 100;
		this.fuelConsumptionRate = calculateFuelConsumption();
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

	private float calculateFuelConsumption() {
		float baseFuelConsumption = (float) 1.0;
		int cantTrip = getTripulantes();

		if (cantTrip <= 1) {
			return baseFuelConsumption;
		} else {
			return baseFuelConsumption * (1 + ((float) 0.15 * (cantTrip - 1)));
		}
	}

	// Ver si lo usamos
	public void updateFuel(float elapsedTime) {
		this.fuelAmount -= this.fuelConsumptionRate * elapsedTime;

		if (this.fuelAmount < 0) {
			this.fuelAmount = 0;
			this.estado = "Sin Combustible";
		}
	}

	public float getFuelAmount() {
		return fuelAmount;
	}

	public float getFuelConsumptionRate() {
		return fuelConsumptionRate;
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
		return this.withObserver ? 300 : 150;
	}

	// Getters for crew configuration
	public boolean isWithPilot() {
		return withPilot;
	}

	public boolean isWithObserver() {
		return withObserver;
	}

	public boolean isWithOperator() {
		return withOperator;
	}
}