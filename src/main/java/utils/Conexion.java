package utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Conexion {

	private String driver, url, user, password;

	public Connection obtenerConexion() {

		Connection con = null;

		try {
			Properties p = new Properties();
			String fileConfig = "config.properties";

			InputStream input = getClass().getClassLoader().getResourceAsStream(fileConfig);

			if (input == null) {
				throw new RuntimeException("No se encontró el archivo " + fileConfig);
			}

			p.load(input);
			driver = p.getProperty("driver");
			url = p.getProperty("url");
			user = p.getProperty("user");
			password = p.getProperty("password");

			Class.forName(driver);
			con = DriverManager.getConnection(url, user, password);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error cargando Driver");
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error obteniendo conexion");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error leyendo properties");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error leyendo properties");
		}

		return con;

	}

}
