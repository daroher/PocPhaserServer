package logica;

public class Jugador {
	private String equipo;
	private int idJugador;
	private ElementoJuego elementoJuego;

	public Jugador() {
		super();
	}

	public Jugador(String equipo, int idJugador) {
		this.equipo = equipo;
		this.idJugador = idJugador;
	}

	public Jugador(String equipo, int idJugador, ElementoJuego elementoJuego) {
		this.equipo = equipo;
		this.idJugador = idJugador;
		this.elementoJuego = elementoJuego;
	}

	public void setEquipo(String equipo) {
		this.equipo = equipo;
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

	public void setElementoJuego(ElementoJuego elementoJuego) {
		this.elementoJuego = elementoJuego;
	}

}