package vo;

import java.util.List;

public class AvionActivoData {

	String estado;
	List<String> tripulantes;
	boolean withPilot;
	boolean withObserver;
	boolean withOperator;
	float fuelAmount;

	public AvionActivoData() {
		super();
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public List<String> getTripulantes() {
		return tripulantes;
	}

	public void setTripulantes(List<String> tripulantes) {
		this.tripulantes = tripulantes;
	}

	public boolean isWithPilot() {
		return withPilot;
	}

	public void setWithPilot(boolean withPilot) {
		this.withPilot = withPilot;
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

	public float getFuelAmount() {
		return fuelAmount;
	}

	public void setFuelAmount(float fuelAmount) {
		this.fuelAmount = fuelAmount;
	}

}
