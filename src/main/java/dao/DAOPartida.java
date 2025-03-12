package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import logica.Francia;
import logica.Partida;
import utils.Consultas;

public class DAOPartida implements IDAOPartida{
	
	public void guardarPartidaEnBD(Connection con, Partida partida) {
	    Consultas cons = new Consultas();
	    String deletePartida = cons.borrarPartidaAnterior();
	    String insertPartida = cons.insertarPartida();

	    PreparedStatement pstmt = null;

	    try {
	        // Borrar partida anterior
	        pstmt = con.prepareStatement(deletePartida);
	        pstmt.executeUpdate();
	        pstmt.close();

	        // Insertar nueva partida
	        pstmt = con.prepareStatement(insertPartida);
	        pstmt.setString(1, partida.getId());
	        pstmt.setString(2, partida.getEstado());

	        String jugadoresJson = new Gson().toJson(partida.getJugadores());
	        pstmt.setString(3, jugadoresJson);

	        Francia francia = partida.getFrancia();
	        pstmt.setFloat(4, francia.getPosX());
	        pstmt.setFloat(5, francia.getPosY());
	        pstmt.setFloat(6, francia.getAngle());

	        pstmt.executeUpdate();
	        System.out.println("Partida guardada correctamente.");
	    } catch (SQLException e) {
	        System.out.println("ERROR: No se pudo guardar la partida.");
	        e.printStackTrace();
	    } finally {
	        try {
	            if (pstmt != null) pstmt.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	public void obtenerPartida(Connection con, JsonObject loadMessage) {
		Consultas cons = new Consultas();
		String sql = cons.obtenerPartida();

		try {

			PreparedStatement ps = con.prepareStatement(sql); 
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				// Construimos el objeto JSON con los datos obtenidos
				loadMessage.addProperty("id", rs.getString("id"));
				loadMessage.addProperty("estado", rs.getString("estado"));
				loadMessage.addProperty("jugadores", rs.getString("jugadores"));

				JsonObject francia = new JsonObject();
				francia.addProperty("x", rs.getFloat("francia_x"));
				francia.addProperty("y", rs.getFloat("francia_y"));
				francia.addProperty("angle", rs.getFloat("francia_angle"));

				loadMessage.add("francia", francia);

				// Creamos el mensaje final con el evento a enviar
				JsonObject message = new JsonObject();
				message.addProperty("event", "CARGAR_JUEGO");
				message.add("data", loadMessage);

			} else {
				System.out.println("No se encontró partida guardada.");
			}
		}catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error obteniendo partida de la BD");
		}
		
	}
	
	public String borrarPartidaAnterior() {
	    return "DELETE FROM partida";
	}

}
