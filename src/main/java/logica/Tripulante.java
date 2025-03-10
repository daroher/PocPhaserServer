package logica;

public enum Tripulante {
	PILOT("pilot"), OBSERVER("observer"), OPERATOR("operator");

	private final String tipo;

	Tripulante(String tipo) {
		this.tipo = tipo;
	}

	public String getTipo() {
		return tipo;
	}

	// Método para convertir String a Tripulante
	public static Tripulante fromTipo(String tipo) {
		for (Tripulante tripulante : Tripulante.values()) {
			if (tripulante.tipo.equalsIgnoreCase(tipo)) {
				return tripulante;
			}
		}
		throw new IllegalArgumentException("Tripulante no válido: " + tipo);
	}
}