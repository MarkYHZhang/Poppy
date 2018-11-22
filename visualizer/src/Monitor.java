public abstract class Monitor {

	public String name;

	public abstract void onData(String str);
	public void render3d() {
	}
	public void render2d(int width, int height) {
	}
	public String text() {
		return null;
	}
	public void keyboard() {
	}
}
