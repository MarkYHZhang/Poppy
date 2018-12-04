import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector3f;

public class MonitorIMU extends Monitor {

	public double ww, xx, yy, zz;
	public double ax, ay, az;
	public double gx, gy, gz;
	public double mx, my, mz;

	public int n = 0;
	public double ggx, ggy, ggz;
	public double lax, lay, laz;
	public double vx, vy;

	public double temp;

	public boolean hidden;

	public ArrayList<Vector3f> list = new ArrayList<>();

	public void onData(String str) {
		String[] splitted = str.split(" ", 2);
		String com = splitted[0];
		if(com.equals("dat")) {
			Scanner scanner = new Scanner(splitted[1]);
			ww = scanner.nextDouble();
			xx = scanner.nextDouble();
			yy = scanner.nextDouble();
			zz = scanner.nextDouble();

			ax = scanner.nextDouble();
			ay = scanner.nextDouble();
			az = scanner.nextDouble();

			gx = scanner.nextDouble();
			gy = scanner.nextDouble();
			gz = scanner.nextDouble();

			lax += (ax - lax) / 10;
			lay += (ay - lay) / 10;
			laz += (az - laz) / 10;

//			ggx += gx;
//			ggy += gy;
//			ggz += gz;
//			n++;

			vx += ax / 500;
			vy += ay / 500;

			System.out.println(ggx / n + ", " + ggy / n + ", " + ggz / n);

			double nmx = scanner.nextDouble();
			double nmy = scanner.nextDouble();
			double nmz = scanner.nextDouble();

			temp = scanner.nextDouble();
			if (nmx != mx || nmy != my || nmz != mz) {
				mx = nmx;
				my = nmy;
				mz = nmz;
				synchronized (list) {
					list.add(new Vector3f((float) mx, (float) my, (float) mz));
				}
			}
		} else if(com.equals("calib")) {
			System.out.println("[imu] received calibration matrix");
		}
	}

	public void render3d() {
		glColor3d(1, 1, 1);

		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT1);

		if(!hidden) {
			glPushMatrix();

			glRotated(Math.toDegrees(Math.acos(ww) * 2), xx, yy, zz);
			glScaled(0.7, 1, 0.05);
			Object3D.drawCube();

			glPopMatrix();
		}

		glPushMatrix();
		glRotated(Math.toDegrees(Math.acos(ww) * 2), xx, yy, zz);
		glColor3d(0, 1, 1);
		Object3D.drawArrow(new Vector3f((float) mx * 25, (float) my * 25, (float) mz * 25));

		glColor3d(1, 1, 0);
		Object3D.drawArrow(new Vector3f((float) lax, (float) lay, (float) laz));
		glPopMatrix();

		glColor3d(1, 1, 0);
		Object3D.drawArrow(new Vector3f((float) vx, (float) vy, (float) 0));
		glPopMatrix();

		glDisable(GL_LIGHTING);
		glDisable(GL_LIGHT1);

		glBegin(GL_POINTS);
		glColor3d(1, 1, 1);

		synchronized(list) {
			for (int i = 0; i < list.size(); ++i) {
				Vector3f v = list.get(i);
				glVertex3d(v.x * 25, v.y * 25, v.z * 25);
			}
		}
		glEnd();

	}

	public void keyboard() {
		switch(Keyboard.getEventKey()) {
			case Keyboard.KEY_H:
				hidden = !hidden;
				break;
			case Keyboard.KEY_C:
				list.clear();
				break;
			case Keyboard.KEY_E:
				try {
					PrintWriter writer = new PrintWriter(new File("compass.csv"));
					for (int i = 0; i < list.size(); ++i) {
						Vector3f v = list.get(i);
						writer.println(v.x + "," + v.y + "," + v.z);
					}
					writer.close();
				} catch(Exception e) {
				}
		}
	}

	public String text() {
		//return String.format("q %12.6f %12.6f %12.6f %12.6f\na %12.6f %12.6f %12.6f\ng %12.6f %12.6f %12.6f\nm %12.6f %12.6f %12.6f", ww, xx, yy, zz, ax, ay, az, gx, gy, gz, mx, my, mz);
		return null;
	}
}
