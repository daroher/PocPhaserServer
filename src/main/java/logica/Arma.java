package logica;

public class Arma {
	private int cantDisparos;
	private int disparosRestantes;
	private String estado;

	public Arma(int cantDisparos, int disparosRestantes, String estado) {
		this.cantDisparos = cantDisparos;
		this.disparosRestantes = disparosRestantes;
		this.estado = estado;
	}

	// Getters y setters
	public int getCantDisparos() {
		return cantDisparos;
	}

	public void setCantDisparos(int cantDisparos) {
		this.cantDisparos = cantDisparos;
	}

	public int getDisparosRestantes() {
		return disparosRestantes;
	}

	public void setDisparosRestantes(int disparosRestantes) {
		this.disparosRestantes = disparosRestantes;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}
}