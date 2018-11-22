import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class Object3D {

	public static int listCube;
	public static int listAxes;
	public static int listGrid;
	public static int listArrow;

	public static void init() {
		listCube = glGenLists(1);
		glNewList(listCube, GL_COMPILE);
			glBegin(GL_QUADS);

			glColor3d(1, 0, 0);
			glNormal3d(-1,  0,  0);
			glVertex3d(-1, -1, -1);
			glVertex3d(-1, -1,  1);
			glVertex3d(-1,  1,  1);
			glVertex3d(-1,  1, -1);

			glColor3d(1, 1, 0);
			glNormal3d( 1,  0,  0);
			glVertex3d( 1, -1, -1);
			glVertex3d( 1, -1,  1);
			glVertex3d( 1,  1,  1);
			glVertex3d( 1,  1, -1);

			glColor3d(0, 1, 0);
			glNormal3d( 0, -1,  0);
			glVertex3d(-1, -1, -1);
			glVertex3d(-1, -1,  1);
			glVertex3d( 1, -1,  1);
			glVertex3d( 1, -1, -1);

			glColor3d(0, 1, 1);
			glNormal3d( 0,  1,  0);
			glVertex3d(-1,  1, -1);
			glVertex3d(-1,  1,  1);
			glVertex3d( 1,  1,  1);
			glVertex3d( 1,  1, -1);

			glColor3d(0, 0, 1);
			glNormal3d( 0,  0, -1);
			glVertex3d(-1, -1, -1);
			glVertex3d(-1,  1, -1);
			glVertex3d( 1,  1, -1);
			glVertex3d( 1, -1, -1);

			glColor3d(1, 0, 1);
			glNormal3d( 0,  0,  1);
			glVertex3d(-1, -1,  1);
			glVertex3d(-1,  1,  1);
			glVertex3d( 1,  1,  1);
			glVertex3d( 1, -1,  1);

			glEnd();
		glEndList();

		listAxes = glGenLists(1);
		glNewList(listAxes, GL_COMPILE);

			glLineWidth(3.0f);
			glBegin(GL_LINES);
			glColor3d(1, 0, 0);
			glVertex3d(0, 0, 0);
			glVertex3d(16, 0, 0);

			glColor3d(0, 1, 0);
			glVertex3d(0, 0, 0);
			glVertex3d(0, 16, 0);

			glColor3d(0, 0, 1);
			glVertex3d(0, 0, 0);
			glVertex3d(0, 0, 16);
			glEnd();

			glLineWidth(1.0f);
			glBegin(GL_LINES);
			glColor3d(0.75, 0, 0);
			glVertex3d(0, 0, 0);
			glVertex3d(-16, 0, 0);

			glColor3d(0, 0.75, 0);
			glVertex3d(0, 0, 0);
			glVertex3d(0, -16, 0);

			glColor3d(0, 0, 0.75);
			glVertex3d(0, 0, 0);
			glVertex3d(0, 0, -16);
			glEnd();

		glEndList();

		listGrid = glGenLists(1);
		glNewList(listGrid, GL_COMPILE);

			glBegin(GL_LINES);
			glColor3d(0.25, 0.25, 0.25);
			for (int i = -16; i <= 16; ++i) {
				if (i == 0)
					continue;
				glVertex3d(i, -16, 0);
				glVertex3d(i, 16, 0);
				glVertex3d(-16, i, 0);
				glVertex3d(16, i, 0);
			}
			glEnd();

		glEndList();

		int slices = 16;
		listArrow = glGenLists(2);
		glNewList(listArrow, GL_COMPILE);
			Cylinder cylinder = new Cylinder();
			cylinder.draw(0.05f, 0.05f, 1f, slices, slices);
		glEndList();

		glNewList(listArrow + 1, GL_COMPILE);
			cylinder.draw(0.1f, 0f, 0.2f, slices, slices);
			glBegin(GL_POLYGON);
			glNormal3d(0, 0, -1);
			for(int i = 0; i < slices; ++i) {
				double ang = -Math.PI * 2 / slices * i;
				glVertex3d(Math.sin(ang) * 0.1, Math.cos(ang) * 0.1, 0);
			}
			glEnd();
		glEndList();

	}
	public static void drawCube() {
		glCallList(listCube);
	}

	public static void drawAxes() {
		glCallList(listAxes);
	}

	public static void drawGrid() {
		glCallList(listGrid);
	}

	public static void drawArrow(Vector3f to) {
		double len = to.length();
		if(len < 1e-6) {
			return;
		}
		glPushMatrix();
		Vector3f expected = new Vector3f(0, 0, 1);
		to.normalise();
		Vector3f cross = new Vector3f();
		Vector3f.cross(expected, to, cross);
		glRotated(Math.toDegrees(Math.acos(Vector3f.dot(expected, to))), cross.x, cross.y, cross.z);
		glScaled(1, 1, len);
		glCallList(listArrow);
		glPopMatrix();

		glPushMatrix();
		glRotated(Math.toDegrees(Math.acos(Vector3f.dot(expected, to))), cross.x, cross.y, cross.z);
		glTranslated(0, 0, len);
		glCallList(listArrow + 1);
		glPopMatrix();
	}


}
