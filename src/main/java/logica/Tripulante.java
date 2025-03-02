package logica;

public enum Tripulante {
	PILOTO("pilot"), OBSERVADOR("observer"), OPERADOR("operator");

	private final String tipo;

	Tripulante(String tipo) {
		this.tipo = tipo;
	}

	public String getTipo() {
		return tipo;
	}
}