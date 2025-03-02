package logica;

public class Jugador {
	private String equipo;
	private int idJugador;
	private ElementoJuego elementoJuego;

	public Jugador(String equipo, int idJugador) {
		this.equipo = equipo;
		this.idJugador = idJugador;
	}

	public Jugador(String equipo, int idJugador, ElementoJuego elementoJuego) {
		this.equipo = equipo;
		this.idJugador = idJugador;
		this.elementoJuego = elementoJuego;
	}

	public String getEquipo() {
		return equipo;
	}

	public int getIdJugador() {
		return idJugador;
	}

	public ElementoJuego getElementoJuego() {
		return elementoJuego;
	}

}