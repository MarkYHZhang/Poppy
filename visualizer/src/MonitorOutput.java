import java.util.Scanner;

import static org.lwjgl.opengl.GL11.*;

public class MonitorOutput extends Monitor {
	public double o1, o2;
	public void onData(String str) {
		Scanner scanner = new Scanner(str);
		o1 = scanner.nextDouble();
		o2 = scanner.nextDouble();
	}

	public void render2d(int width, int height) {
		glTranslated(width - 50, 5, 0);
		drawBar(o1);

		glTranslated(25, 0, 0);
		drawBar(o2);
	}

	public void drawBar(double v) {
		glBegin(GL_LINE_LOOP);
		glVertex2d(0, 0);
		glVertex2d(0, 100);
		glVertex2d(20, 100);
		glVertex2d(20, 0);
		glEnd();

		glBegin(GL_QUADS);
		glVertex2d(0, 50 - v * 50);
		glVertex2d(0, 50);
		glVertex2d(20, 50);
		glVertex2d(20, 50 - v * 50);
		glEnd();
	}
}
