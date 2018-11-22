public abstract class GuiComponent {

	public int x, y, width, height;

	public GuiComponent() {
	}

	public void mousePress(int x, int y, int button) {
	}

	public void mouseRelease(int x, int y, int button) {
	}

	public void mouseMove(int x, int y) {
	}

	public void mouseWheel(int x, int y, int wheel) {
	}

	public void mouseLeave() {
	}

	public void keyPress() {
	}

	public void keyRelease() {
	}

	public void keyType() {
	}

	public void input(double dt) {
	}

	public void resize() {
	}

	public void render() {
	}

	public void focus() {
	}

	public void unfocus() {
	}
}
