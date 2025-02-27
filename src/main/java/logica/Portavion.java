package logica;

import java.util.ArrayList;
import java.util.List;

public class Portavion extends ElementoJuego {
	private int cantAviones;
	private List<AvionBritanico> aviones;

	public Portavion(float posX, float posY, float angle) {
		super(posX, posY, angle);
		this.cantAviones = 10;
		this.aviones = new ArrayList<>();

		for (int i = 0; i < cantAviones; i++) {
			AvionBritanico avion = new AvionBritanico(posX, posY, angle, "Funcional");
			aviones.add(avion);
		}
	}

	public AvionBritanico obtenerAvionDisponible() {
		for (AvionBritanico avion : aviones) {
			if (avion.getEstado().equals("Funcional")) {
				return avion;
			}
		}
		return null; // No hay aviones disponibles
	}

	public int getCantAviones() {
		return cantAviones;
	}

	public void setCantAviones(int cantAviones) {
		this.cantAviones = cantAviones;
	}

	public List<AvionBritanico> getAviones() {
		return aviones;
	}

	public void setAviones(List<AvionBritanico> aviones) {
		this.aviones = aviones;
		this.cantAviones = aviones.size();
	}

	@Override
	public float getVisionRadius() {
		return 0;
	}
}