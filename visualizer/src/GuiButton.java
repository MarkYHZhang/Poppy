import static org.lwjgl.opengl.GL11.*;

public class GuiButton extends GuiComponent {
	public String text;

	public boolean pressed;
	public boolean hover;
	public ButtonAction action;

	public GuiButton(String text, ButtonAction action) {
		this.text = text;
		this.action = action;
	}

	public void render() {
		if(pressed) {
			glColor4d(0.25, 0.25, 0.25, 1);
		} else if(hover) {
			glColor4d(0.25, 0.25, 0.25, 0.75);
		} else {
			glColor4d(0.25, 0.25, 0.25, 0.5);
		}
		glBegin(GL_QUADS);
		glVertex2d(0, 0);
		glVertex2d(0, height);
		glVertex2d(width, height);
		glVertex2d(width, 0);
		glEnd();
		glColor3d(1, 1, 1);
		FontUtil.drawCenterText(text, FontUtil.font16, width / 2, height / 2);
	}

	public void mousePress(int x, int y, int button) {
		pressed = true;
		if(action != null) {
			action.onAction();
		}
	}

	public void mouseMove(int x, int y) {
		hover = true;
	}

	public void mouseRelease(int x, int y, int button) {
		pressed = false;
	}

	public void mouseLeave() {
		hover = false;
	}
	public interface ButtonAction {
		void onAction();
	}
}
