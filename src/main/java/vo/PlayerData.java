package vo;

public class PlayerData {
	float x;
	float y;
	float angle;
	float visionRadius;
	String team;
	BismarckData bismarck;
	PortavionData portavion;
	AvionActivoData avionActivo;

	public PlayerData() {
		super();
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getAngle() {
		return angle;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getVisionRadius() {
		return visionRadius;
	}

	public void setVisionRadius(float visionRadius) {
		this.visionRadius = visionRadius;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public BismarckData getBismarck() {
		return bismarck;
	}

	public void setBismarck(BismarckData bismarck) {
		this.bismarck = bismarck;
	}

	public PortavionData getPortavion() {
		return portavion;
	}

	public void setPortavion(PortavionData portavion) {
		this.portavion = portavion;
	}

	public AvionActivoData getAvionActivo() {
		return avionActivo;
	}

	public void setAvionActivo(AvionActivoData avionActivo) {
		this.avionActivo = avionActivo;
	}
}
