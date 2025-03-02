package logica;

public class Bismarck extends ElementoJuego {
	private int vidaRestante;
	private int defaultSpeed;
	private Arma arma;

	public Bismarck(float posX, float posY, float angle, int vidaRestante) {
		super(posX, posY, angle);
		this.vidaRestante = vidaRestante;
		this.defaultSpeed = 2;
		// Crear un arma con disparos infinitos
		this.arma = new Arma(Integer.MAX_VALUE, Integer.MAX_VALUE, "Funcional");
	}

	public int getVidaRestante() {
		return vidaRestante;
	}

	public void setVidaRestante(int vidaRestante) {
		this.vidaRestante = vidaRestante;
	}

	@Override
	public float getVisionRadius() {
		return 500;
	}

	public float getSpeed() {
		return this.defaultSpeed;
	}

	public boolean disparar() {
		if (arma.getEstado().equals("Funcional")) {
			// Como tiene disparos infinitos, no es necesario decrementar
			return true;
		}
		return false;
	}

	public Arma getArma() {
		return arma;
	}

}