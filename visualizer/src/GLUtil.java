import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Stack;

import static org.lwjgl.opengl.GL11.*;

public class GLUtil {

	public static Stack<Rectangle> regions = new Stack<>();

	static {
		regions.push(null);
	}

	public static int circleList;

	public static void init() {
		circleList = glGenLists(1);
		glNewList(circleList, GL_COMPILE);
		glBegin(GL_POLYGON);
		int sec = 64;
		double ang = 2 * Math.PI / sec;
		for(int i = 0; i < sec; ++i) {
			glVertex2d(Math.cos(ang * i), Math.sin(ang * i));
		}
		glEnd();
		glEndList();
	}

	public static int loadTexture(InputStream stream) {
		try {
			System.out.println("Loading texture");

			BufferedImage image = ImageIO.read(stream);
			stream.close();
			DataBufferByte buffer = (DataBufferByte) image.getRaster().getDataBuffer();
			byte[] data = buffer.getData();
			ByteBuffer buf = BufferUtils.createByteBuffer(data.length);
			for (int i = 0; i < data.length / 4; ++i) {
				buf.put(data[i * 4 + 1]).put(data[i * 4 + 2]).put(data[i * 4 + 3]).put(data[i * 4]);
			}
			buf.flip();

			int texId = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, texId);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);

			return texId;

		} catch (Exception e) {
			System.out.println("Failed to load texture");
			e.printStackTrace();
		}
		return 0;
	}

	public static void quad(double x, double y, double w, double h) {
		glBegin(GL_QUADS);
		glVertex2d(x, y);
		glVertex2d(x, y + h);
		glVertex2d(x + w, y + h);
		glVertex2d(x + w, y);
		glEnd();
	}

	public static void circle(int x, int y, int r) {
		glPushMatrix();
		glTranslated(x, y, 0);
		glScaled(r, r, 0);
		glCallList(circleList);
		glPopMatrix();
	}

	public static void setRegion(Rectangle r) {
		glViewport(r.x, Display.getHeight() - r.y - r.height, r.width, r.height);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, r.width, r.height, 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
	}

	public static Rectangle getRegion() {
		Rectangle r = regions.peek();
		if(r == null) {
			return new Rectangle(0, 0, Display.getWidth(), Display.getHeight());
		}
		return r;
	}

	public static void pushRegion(int x, int y, int width, int height) {
		Rectangle rp = getRegion();
		Rectangle nr = new Rectangle(rp.x + x, rp.y + y, width, height);
		Rectangle curr = nr.intersection(rp);
		setRegion(curr);
		regions.push(curr);
	}

	public static void popRegion() {
		regions.pop();
		setRegion(getRegion());
	}
}
