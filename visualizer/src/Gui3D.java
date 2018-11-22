import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public class Gui3D extends GuiComponent {

	public double x = -2, y = -2, z = -2;
	public double rx = -45, ry = 45;
	public double speed = 5;

	public Gui3D() {
		glEnable(GL_LIGHT1);
		glEnable(GL_LIGHTING);
		glLight(GL_LIGHT1, GL_AMBIENT, (FloatBuffer) BufferUtils.createFloatBuffer(4).put(0.1f).put(0.1f).put(0.1f).put(1).flip());
		glLight(GL_LIGHT1, GL_DIFFUSE, (FloatBuffer) BufferUtils.createFloatBuffer(4).put(1).put(1).put(1).put(1).flip());
		glDisable(GL_LIGHT1);
		glDisable(GL_LIGHTING);

		glColorMaterial(GL_FRONT, GL_DIFFUSE);
		glEnable(GL_COLOR_MATERIAL);
		glEnable(GL_NORMALIZE);
	}

	public void render() {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(80, (float) width / (float) height, 0.01f, 1000.0f);
		glMatrixMode(GL_MODELVIEW);
		glEnable(GL_DEPTH_TEST);

		glLoadIdentity();
		glMultMatrix((DoubleBuffer) BufferUtils.createDoubleBuffer(16).put(new double[]{0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1}).flip());
		glRotated(ry, 0, 1, 0);
		glRotated(rx, 0, 0, 1);

		glLight(GL_LIGHT1, GL_POSITION, (FloatBuffer) BufferUtils.createFloatBuffer(4).put(0).put(0).put(0).put(1).flip());
		glTranslated(x, y, z);

		Object3D.drawGrid();
		Object3D.drawAxes();

		for(Monitor m : QuaternionVisualizer.monitorList) {
			m.render3d();
		}

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, width, height, 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glDisable(GL_DEPTH_TEST);

		glLoadIdentity();

		int y = 0;
		for(Monitor m : QuaternionVisualizer.monitorList) {
			String txt = m.text();

			if(txt != null) {
				FontUtil.drawText(m.name + ": ", FontUtil.font16, 0, y);
				Dimension d1 = FontUtil.getTextDimension(m.name + ": ", FontUtil.font16);
				FontUtil.drawText(txt, FontUtil.font16, d1.getWidth(), y);
				Dimension d2 = FontUtil.getTextDimension(txt, FontUtil.font16);
				y += d2.height + 10;
			}

			glPushMatrix();
			m.render2d(width, height);
			glPopMatrix();
		}
	}

	public void input(double dt) {
		double mvmt = speed * dt;

		double forward = 0;
		double right = 0;
		double up = 0;

		if (Keyboard.isKeyDown(Keyboard.KEY_A))
			right -= 1;
		if (Keyboard.isKeyDown(Keyboard.KEY_S))
			forward -= 1;
		if (Keyboard.isKeyDown(Keyboard.KEY_D))
			right += 1;
		if (Keyboard.isKeyDown(Keyboard.KEY_W))
			forward += 1;
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
			up -= 1;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
			up += 1;

		if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			QuaternionVisualizer.angle.value += 0.05;
			QuaternionVisualizer.angle.listener.onChanged(QuaternionVisualizer.angle.value);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			QuaternionVisualizer.angle.value -= 0.05;
			QuaternionVisualizer.angle.listener.onChanged(QuaternionVisualizer.angle.value);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			QuaternionVisualizer.handler.sendDouble(6, -0.4);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			QuaternionVisualizer.handler.sendDouble(6, 0.4);
		}

		if(!Keyboard.isKeyDown(Keyboard.KEY_UP) && !Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			QuaternionVisualizer.handler.sendDouble(6, 0);
		}

		double ang = Math.toRadians(rx);

		x += (Math.cos(ang) * forward - Math.sin(ang) * right) * mvmt;
		y -= (Math.sin(ang) * forward + Math.cos(ang) * right) * mvmt;
		z += up * mvmt;

	}

	public void keyPress() {
		int key = Keyboard.getEventKey();

		if(key == Keyboard.KEY_ESCAPE) {
			MouseHelper.setGrabbed(false);
		}

		for(Monitor m : QuaternionVisualizer.monitorList) {
			m.keyboard();
		}
	}

	public void mousePress(int x, int y, int button) {
		if(button == 0) {
			MouseHelper.setGrabbed(true);
		}
	}

	public void mouseMove(int x, int y) {
		if(Mouse.isGrabbed()) {
			rx += MouseHelper.getDX() * 0.2;
			ry += MouseHelper.getDY() * 0.2;

			if (ry > 90)
				ry = 90;
			if (ry < -90)
				ry = -90;
		}
	}

	public void mouseWheel(int x, int y, int wheel) {
		speed *= Math.pow(2, wheel / 120.0 * 0.1);
		if(speed > 50) {
			speed = 50;
		}
	}
}
