package logica;

public class GameEvent {
	private String action;
	private String team;
	private float x = -1000; // inicia fuera del mapa para evitar guerras en eje (0,0), mejorar
	private float y = -1000; // inicia fuera del mapa para evitar guerras en eje (0,0), mejorar
	private float visionRadius;
	private float angle;
	private float relativeAngle;
	private boolean withPilot;
	private boolean withObserver;
	private boolean withOperator;
	private float vsRelativeDistanceX;
	private float vsDistanceZ;
	private boolean isPlaneActive;

	public boolean isWithPilot() {
		return withPilot;
	}

	public void setWithPilot(boolean withPilot) {
		this.withPilot = withPilot;
	}

	public boolean isWithObserver() {
		return withObserver;
	}

	public void setWithObserver(boolean withObserver) {
		this.withObserver = withObserver;
	}

	public boolean isWithOperator() {
		return withOperator;
	}

	public void setWithOperator(boolean withOperator) {
		this.withOperator = withOperator;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setVisionRadius(float visionRadius) {
		this.visionRadius = visionRadius;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public void setRelativeAngle(float relativeAngle) {
		this.relativeAngle = relativeAngle;
	}

	public String getAction() {
		return action;
	}

	public String getTeam() {
		return team;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getVisionRadius() {
		return visionRadius;
	}

	public float getAngle() {
		return angle;
	}

	public float getRelativeAngle() {
		return relativeAngle;
	}

	public int getTripulantes() {
		int count = 0;
		if (withPilot)
			count++;
		if (withObserver)
			count++;
		if (withOperator)
			count++;
		return count;
	}

	public float getVsRelativeDistanceX() {
		return vsRelativeDistanceX;
	}

	public void setVsRelativeDistanceX(float vsRelativeDistanceX) {
		this.vsRelativeDistanceX = vsRelativeDistanceX;
	}

	public float getVsDistanceZ() {
		return vsDistanceZ;
	}

	public void setVsDistanceZ(float vsDistanceZ) {
		this.vsDistanceZ = vsDistanceZ;
	}
	
	public boolean isPlaneActive() {
		return this.isPlaneActive;
	}

	public void setIsPlaneActive(boolean isPlaneActive) {
		this.isPlaneActive = isPlaneActive;
	}

}
