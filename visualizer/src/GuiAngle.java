import static org.lwjgl.opengl.GL11.*;

public class GuiAngle extends GuiComponent {
	public String text;

	public boolean pressed;
	public boolean clamp = true;

	public double value;

	public int font = FontUtil.font16;
	public TextGenerator generator = (v) -> String.format("%.1f", v / Math.PI * 180);

	public int arm_len;

	public ChangeListener listener;

	public GuiAngle() {
	}

	public GuiAngle(ChangeListener listener) {
		this.listener = listener;
	}

	public void resize() {
		arm_len = Math.min(width, height) / 2;
	}

	public void render() {
		glColor4d(0.25, 0.25, 0.25, 1);
		GLUtil.circle(width / 2, height / 2, arm_len);

		glColor4d(0, 0.5, 1, 0.5);
		glPushMatrix();
		glTranslated(width / 2, height / 2, 0);
		glRotated(value / Math.PI * 180 - 90, 0, 0, -1);
		GLUtil.quad(-5, -arm_len, 10, arm_len);
		glPopMatrix();

		glColor3d(1, 1, 1);
		FontUtil.drawCenterText(generator.genText(value), font, width / 2, height / 2);
	}

	public void mousePress(int x, int y, int button) {
		pressed = true;
		mouseMove(x, y);
	}

	public void mouseMove(int x, int y) {
		if(pressed) {
			value = -Math.atan2(y - height / 2, x - width / 2);
			if(listener != null) {
				listener.onChanged(value);
			}
		}
	}

	public void mouseRelease(int x, int y, int button) {
		pressed = false;
	}

	public interface TextGenerator {
		String genText(double value);
	}

	public interface ChangeListener {
		void onChanged(double value);
	}
}
