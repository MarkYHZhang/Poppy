import org.lwjgl.BufferUtils;

import java.awt.*;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class FontUtil {

	public static int font16 = 0;
	public static int font24 = 1;
	public static int font36 = 2;
	public static int font48 = 3;

	public static int[] font = new int[4];

	public static final Dimension[] fontMetric = {
			new Dimension(10, 19),
			new Dimension(14, 28),
			new Dimension(22, 43),
			new Dimension(29, 57),
	};

	public static void init() {
		font[font16] = GLUtil.loadTexture(FontUtil.class.getResourceAsStream("/fonts/font16.png"));
		font[font24] = GLUtil.loadTexture(FontUtil.class.getResourceAsStream("/fonts/font24.png"));
		font[font36] = GLUtil.loadTexture(FontUtil.class.getResourceAsStream("/fonts/font36.png"));
		font[font48] = GLUtil.loadTexture(FontUtil.class.getResourceAsStream("/fonts/font48.png"));
	}

	public static Dimension getTextDimension(String s, int fontInd) {
		int cp;

		int x = 0, y = 0;

		int mx = 0;

		for(int i = 0; i < s.length(); i += Character.charCount(cp)) {
			cp = s.codePointAt(i);

			if (cp == '\n') {
				y++;
				x = 0;
			}

			if (cp == '\t') {
				x = x / 4 * 4 + 4;
			}

			if (cp < 0x20 || cp > 0x7F) {
				continue;
			}

			mx = Math.max(mx, ++x);
		}

		return new Dimension(mx * fontMetric[fontInd].width, (y + 1) * fontMetric[fontInd].height);
	}

	public static void drawText(String s, int fontInd, double x, double y) {
		glPushMatrix();
		glTranslated(x, y, 0);
		FontUtil.drawText(s, fontInd);
		glPopMatrix();
	}

	public static void drawCenterText(String s, int fontInd, double x, double y) {
		Dimension dim = FontUtil.getTextDimension(s, fontInd);
		drawText(s, fontInd, x - dim.getWidth() / 2, y - dim.getHeight() / 2);
	}

	public static void drawText(String s, int fontInd) {

		int cnt = s.codePointCount(0, s.length());

		FloatBuffer vertex = BufferUtils.createFloatBuffer(cnt * 8);
		FloatBuffer texCoord = BufferUtils.createFloatBuffer(cnt * 8);

		int x = 0;
		int y = 0;

		int cp;

		for(int i = 0; i < s.length(); i += Character.charCount(cp)) {
			cp = s.codePointAt(i);

			if(cp == '\n') {
				y++;
				x = 0;
			}
			if(cp == '\t') {
				x = x / 4 * 4 + 4;
			}

			if(cp < 0x20 || cp > 0x7F) {
				continue;
			}

			int width = fontMetric[fontInd].width;
			int height = fontMetric[fontInd].height;

			float verX = x * width;
			float verY = y * height;

			float texWidth = 1 / 16.0f;
			float texHeight = 1 / 6.0f;

			cp -= 0x20;

			float texX = cp % 16 * texWidth;
			float texY = cp / 16 * texHeight;

			vertex.put(verX).put(verY);
			vertex.put(verX).put(verY + height);
			vertex.put(verX + width).put(verY + height);
			vertex.put(verX + width).put(verY);

			texCoord.put(texX).put(texY);
			texCoord.put(texX).put(texY + texHeight);
			texCoord.put(texX + texWidth).put(texY + texHeight);
			texCoord.put(texX + texWidth).put(texY);

			x++;
		}

		int length = vertex.position();

		vertex.flip();
		texCoord.flip();

		glPushAttrib(GL_ALL_ATTRIB_BITS);

		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, font[fontInd]);

		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);

		glVertexPointer(2, 0, vertex);
		glTexCoordPointer(2, 0, texCoord);

		glDrawArrays(GL_QUADS, 0, length / 2);

		glDisableClientState(GL_VERTEX_ARRAY);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);

		glPopAttrib();

	}
}
