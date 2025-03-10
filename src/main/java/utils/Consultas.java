package utils;

public class Consultas {

	public Consultas() {

	}

	public String insertarPartida() {
		String query = "INSERT INTO BistmarckDB.Partidas (id, estado, jugadores, francia_x, francia_y, francia_angle) VALUES (?, ?, ?, ?, ?, ?)";
		return query;
	}

	public String obtenerPartida() {
		String query = "SELECT id, estado, jugadores, francia_x, francia_y, francia_angle FROM BistmarckDB.Partidas ORDER BY id DESC LIMIT 1";
		return query;
	}

	public String borrarPartidaAnterior() {
		String query = "DELETE FROM BistmarckDB.partidas";
		return query;
	}

}