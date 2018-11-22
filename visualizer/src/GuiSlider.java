import org.lwjgl.input.Mouse;

import static org.lwjgl.opengl.GL11.*;

public class GuiSlider extends GuiComponent {
	public String text;

	public boolean pressed;
	public boolean clamp = true;

	public double value;
	public double min = -1, max = 1;
	public double step = 0.01;

	public int font = FontUtil.font16;
	public TextGenerator generator = (v) -> String.format("%.2f", v);

	public GuiSlider() {
	}

	public void render() {
		glColor4d(0.25, 0.25, 0.25, 1);
		GLUtil.quad(0, 0, width, height);

		glColor4d(0, 0.5, 1, 0.5);
		double mapped = (value - min) / (max - min);
		GLUtil.quad(mapped * width - 5, 0, 10, height);

		glColor3d(1, 1, 1);
		FontUtil.drawCenterText(generator.genText(value), font, width / 2, height / 2);
	}

	public void mousePress(int x, int y, int button) {
		MouseHelper.setGrabbed(true);
		pressed = true;
	}

	public void mouseMove(int x, int y) {
		if(pressed) {
			value += MouseHelper.getEventDX() * step;
			if(clamp) {
				value = Math.max(min, Math.min(max, value));
			}
		}
	}

	public void mouseRelease(int x, int y, int button) {
		MouseHelper.setGrabbed(false);
		pressed = false;
	}

	public interface TextGenerator {
		String genText(double value);
	}
}
