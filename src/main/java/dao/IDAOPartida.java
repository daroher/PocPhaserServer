package dao;

import java.sql.Connection;

import com.google.gson.JsonObject;

import logica.Partida;

public interface IDAOPartida {

	public void guardarPartidaEnBD(Connection con, Partida partida);

	public void obtenerPartida(Connection con, JsonObject loadMessage);
}
