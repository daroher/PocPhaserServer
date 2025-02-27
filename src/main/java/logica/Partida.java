package logica;

import java.util.ArrayList;
import java.util.List;

public class Partida {
	private String id;
	private String estado;
	private List<Jugador> jugadores;

	public Partida(String id) {
		this.id = id;
		this.estado = "Esperando";
		this.jugadores = new ArrayList<>();
	}

	public void agregarJugador(Jugador jugador) {

		// Verificar que no haya otro jugador del mismo equipo
		for (Jugador j : jugadores) {
			if (j.getEquipo().equals(jugador.getEquipo())) {
				throw new IllegalArgumentException("Ya existe un jugador del equipo " + jugador.getEquipo());
			}
		}

		jugadores.add(jugador);

		if (jugadores.size() == 2) {
			this.estado = "Iniciada";
		}
	}

	public void iniciarPartida() {
		if (jugadores.size() < 2) {
			throw new IllegalStateException("No hay suficientes jugadores para iniciar la partida");
		}

		this.estado = "Iniciada";
	}

	public void finalizarPartida(String equipoGanador) {
		this.estado = "Finalizada";
	}

	// Getters y setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public List<Jugador> getJugadores() {
		return jugadores;
	}

	public void setJugadores(List<Jugador> jugadores) {
		this.jugadores = jugadores;
	}
}