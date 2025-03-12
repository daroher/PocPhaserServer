package logica;

import java.util.ArrayList;
import java.util.List;

public class AvionBritanico extends ElementoJuego {
	private List<Tripulante> tripulantes;
	private String estado;
	private int defaultSpeed;
	private Arma arma;
	float fuelAmount;

	public AvionBritanico(float posX, float posY, float angle, String estado) {
		super(posX, posY, angle);
		this.tripulantes = new ArrayList<>();
		this.estado = estado;
		this.defaultSpeed = 3;
		this.arma = new Arma(1, 1, "Funcional"); // El avión empieza con 1 disparo

	}

	public void agregarTripulante(Tripulante tripulante) {
		boolean tienePiloto = tripulantes.stream().anyMatch(t -> t.getTipo().equals("piloto"));

		if (!tienePiloto && !tripulante.getTipo().equals("piloto") && tripulantes.size() >= 1) {
			throw new IllegalStateException("Es obligatorio que tenga un tripulante de tipo piloto");
		}

		tripulantes.add(tripulante);
	}

	public boolean withPilot() {
		return tripulantes.stream().anyMatch(t -> t.getTipo().equals("piloto"));
	}

	public boolean withObserver() {
		return tripulantes.stream().anyMatch(t -> t.getTipo().equals("observador"));
	}

	public boolean withOperator() {
		return tripulantes.stream().anyMatch(t -> t.getTipo().equals("operador"));
	}

	public float getSpeed() {
		if (this.getTripulantes().size() == 1) {
			return this.defaultSpeed;
		}

		double decrement = this.getTripulantes().size() * 0.20; // decremento

		if (decrement > 1.0) {
			decrement = 1.0;
		}

		return (float) (defaultSpeed * (1 - decrement));
	}

	@Override
	public float getVisionRadius() {
		return this.withObserver() ? 500 : 300;
	}

	public List<Tripulante> getTripulantes() {
		return tripulantes;
	}

	public void setTripulantes(List<Tripulante> tripulantes) {
		this.tripulantes = tripulantes;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public void disparar() {
		if (arma.getDisparosRestantes() > 0) {
			arma.setDisparosRestantes(arma.getDisparosRestantes() - 1);
			arma.setEstado("Sin balas");
			this.setEstado("Sin balas");
		}
	}

	public float getFuelAmount() {
		return fuelAmount;
	}

	public void setFuelAmount(float fuelAmount) {
		this.fuelAmount = fuelAmount;
	}

}