import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;

public class GuiTextfield extends GuiComponent {

	public ArrayList<StringBuilder> lines = new ArrayList<>();

	public int font = FontUtil.font16;
	public int cursorX, cursorY;
	public int cursorSelX, cursorSelY;
	public int persistCursorX = -1;
	public boolean focused;
	public boolean mouseDown;
	public boolean isShiftDown;
	public boolean isCtrlDown;
	public int scrollX, scrollY;
	public double actScrollX, actScrollY;
	public boolean multiLine = true;

	public GuiTextfield() {
	}

	public GuiTextfield(String text) {
		setText(text);
	}

	public void setText(String text) {
		String[] splitted = text.split("\n", -1);
		lines.clear();
		for(String s : splitted) {
			lines.add(new StringBuilder(s));
		}
	}

	public String getText() {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(StringBuilder s : lines) {
			if(first) {
				result.append('\n');
				first = false;
			}
			result.append(s);
		}
		return result.toString();
	}

	public void render() {

		Dimension fm = FontUtil.fontMetric[font];

		glColor4d(0.3, 0.3, 0.3, 1);
		GLUtil.quad(0, 0, width, height);
		glColor3d(1, 1, 1);

		actScrollX += (scrollX - actScrollX) / 2;
		actScrollY += (scrollY - actScrollY) / 2;
		glTranslated(actScrollX, actScrollY, 0);

		if(focused) {
			glColor3d(0, 0, 0.5);
			int minX, minY;
			int maxX, maxY;
			boolean isCursorAfter = cursorY > cursorSelY || cursorY == cursorSelY && cursorX > cursorSelX;
			if(isCursorAfter) {
				minX = cursorSelX;
				minY = cursorSelY;
				maxX = cursorX;
				maxY = cursorY;
			} else {
				minX = cursorX;
				minY = cursorY;
				maxX = cursorSelX;
				maxY = cursorSelY;
			}
			if(maxY > minY + 1) {
				GLUtil.quad(-actScrollX, (minY + 1) * fm.height, width, fm.height * (maxY - minY - 1));
			}
			if(maxY > minY) {
				GLUtil.quad(minX * fm.width, minY * fm.height, width - minX * fm.width - actScrollX, fm.height);
				GLUtil.quad(0, maxY * fm.height, maxX * fm.width, fm.height);
			} else {
				GLUtil.quad(minX * fm.width, maxY * fm.height, (maxX - minX) * fm.width, fm.height);
			}

			glColor3d(1, 1, 1);
			GLUtil.quad(cursorX * fm.width - 1, cursorY * fm.height, 2, fm.height);

		}

		for(int i = 0; i < lines.size(); ++i) {
			FontUtil.drawText(lines.get(i).toString(), font, 0, i * fm.height);
		}

	}

	public void mousePress(int x, int y, int button) {
		if(button == 0) {
			mouseDown = true;
		}

		Dimension fm = FontUtil.fontMetric[font];

		if(button == 0) {
			cursorSelY = cursorY = Math.max(Math.min((y - (int) actScrollY) / fm.height, lines.size() - 1), 0);
			persistCursorX = cursorSelX = cursorX = Math.max(Math.min((x - (int) actScrollX + fm.width / 2) / fm.width, lines.get(cursorY).length()), 0);
		}
	}

	public void mouseRelease(int x, int y, int button) {
		if(button == 0) {
			mouseDown = false;
		}
	}

	public void mouseMove(int x, int y) {
		Dimension fm = FontUtil.fontMetric[font];

		if(mouseDown) {
			cursorY = Math.max(Math.min((y - (int) actScrollY) / fm.height, lines.size() - 1), 0);
			persistCursorX = cursorX = Math.max(Math.min((x - (int) actScrollX + fm.width / 2) / fm.width, lines.get(cursorY).length()), 0);
			checkScrollBounds();
		}
	}

	public boolean hasSelection() {
		return cursorX != cursorSelX || cursorY != cursorSelY;
	}

	public void deleteSelection() {
		int minX, minY;
		int maxX, maxY;
		boolean isCursorAfter = cursorY > cursorSelY || cursorY == cursorSelY && cursorX > cursorSelX;
		if(isCursorAfter) {
			minX = cursorSelX;
			minY = cursorSelY;
			maxX = cursorX;
			maxY = cursorY;
		} else {
			minX = cursorX;
			minY = cursorY;
			maxX = cursorSelX;
			maxY = cursorSelY;
		}
		if(maxY > minY + 1) {
			int cnt = 0;
			for(int i = minY + 1; i <= maxY - 1; ++i) {
				lines.remove(minY + 1);
				++cnt;
			}
			maxY -= cnt;
		}
		if(maxY > minY) {
			StringBuilder line = lines.get(minY);
			line.delete(minX, line.length());
			line.append(lines.get(maxY).delete(0, maxX));
			lines.remove(maxY);
		} else {
			lines.get(maxY).delete(minX, maxX);
		}

		cursorSelX = cursorX = minX;
		cursorSelY = cursorY = minY;
	}

	public void checkScrollBounds(int overscroll) {
		Dimension fm = FontUtil.fontMetric[font];

		int dx = -Math.max(scrollX + cursorX * fm.width - width, 0);
		int dy = -Math.max(scrollY + (cursorY + 1) * fm.height - height, 0);
		dx += Math.max(-(scrollX + cursorX * fm.width), 0);
		dy += Math.max(-(scrollY + cursorY * fm.height), 0);
		if(dx > 0) {
			dx += overscroll * fm.width;
		} else if(dx < 0) {
			dx -= overscroll * fm.width;
		}
		if(dy > 0) {
			dy += overscroll * fm.height;
		} else if(dy < 0) {
			dy -= overscroll * fm.height;
		}
		scrollX = Math.min(scrollX + dx, 0);
		scrollY = Math.min(scrollY + dy, 0);
	}

	public void checkScrollBounds() {
		checkScrollBounds(0);
	}

	public void updateCursor() {
		if(!isShiftDown) {
			cursorSelX = cursorX;
			cursorSelY = cursorY;
		}
		checkScrollBounds(3);
	}

	public void keyType() {
		char c = Keyboard.getEventCharacter();
		int key = Keyboard.getEventKey();
		if(key == Keyboard.KEY_BACK) {
			if(!hasSelection()) {
				StringBuilder line = lines.get(cursorY);
				if (cursorX == 0) {
					if (cursorY > 0) {
						StringBuilder prevLine = lines.get(cursorY - 1);
						cursorX = prevLine.length();
						prevLine.append(line);
						lines.remove(cursorY);
						cursorY--;
					}
				} else {
					line.deleteCharAt(cursorX - 1);
					cursorX--;
				}
			} else {
				deleteSelection();
			}
			persistCursorX = cursorX;
			cursorSelX = cursorX;
			cursorSelY = cursorY;
			checkScrollBounds(5);
		} else if(key == Keyboard.KEY_DELETE) {
			if(!hasSelection()) {
				StringBuilder line = lines.get(cursorY);
				if (cursorX == line.length()) {
					if (cursorY < lines.size() - 1) {
						StringBuilder nextLine = lines.get(cursorY + 1);
						line.append(nextLine);
						lines.remove(cursorY + 1);
					}
				} else {
					line.deleteCharAt(cursorX);
				}
				cursorSelX = cursorX;
				cursorSelY = cursorY;
			} else {
				deleteSelection();
			}
			checkScrollBounds();
		} else if(key == Keyboard.KEY_RETURN) {
			if(multiLine) {
				if (!hasSelection()) {
					StringBuilder line = lines.get(cursorY);
					lines.add(cursorY + 1, new StringBuilder(line.substring(cursorX)));
					line.delete(cursorX, line.length());
					persistCursorX = cursorX = 0;
					cursorY++;
					cursorSelX = cursorX;
					cursorSelY = cursorY;
				} else {
					deleteSelection();
				}
				checkScrollBounds();
			}
		} else if(key == Keyboard.KEY_UP) {
			if(cursorY > 0) {
				cursorY --;
				cursorX = Math.min(persistCursorX, lines.get(cursorY).length());
			} else {
				persistCursorX = cursorX = 0;
			}
			updateCursor();
		} else if(key == Keyboard.KEY_DOWN) {
			if(cursorY < lines.size() - 1) {
				cursorY ++;
				cursorX = Math.min(persistCursorX, lines.get(cursorY).length());
			} else {
				persistCursorX = cursorX = lines.get(cursorY).length();
			}
			updateCursor();
		} else if(key == Keyboard.KEY_LEFT) {
			if(cursorX == 0) {
				if(cursorY > 0) {
					cursorX = lines.get(cursorY - 1).length();
					cursorY --;
				}
			} else {
				cursorX --;
			}
			persistCursorX = cursorX;
			updateCursor();
		} else if(key == Keyboard.KEY_RIGHT) {
			int len = lines.get(cursorY).length();
			if(cursorX == len) {
				if(cursorY < lines.size() - 1) {
					cursorX = 0;
					cursorY ++;
				}
			} else {
				cursorX ++;
			}
			updateCursor();
		} else if(key == Keyboard.KEY_HOME) {
			persistCursorX = cursorX = 0;
			updateCursor();
		} else if(key == Keyboard.KEY_END) {
			persistCursorX = cursorX = lines.get(cursorY).length();
			updateCursor();
		} else if(32 <= c && c < 127) {
			if(hasSelection()) {
				deleteSelection();
			}
			lines.get(cursorY).insert(cursorX, c);
			cursorX ++;
			cursorSelX = cursorX;
			persistCursorX = cursorX;
			checkScrollBounds();
		}

		if(key == Keyboard.KEY_A) {
			if(isCtrlDown) {
				cursorSelX = cursorSelY = 0;
				cursorY = lines.size() - 1;
				cursorX = lines.get(cursorY).length();
			}
			checkScrollBounds();
		}
	}

	public void mouseWheel(int x, int y, int wheel) {
		if(wheel != 0) {
			scrollY += wheel / 120 * FontUtil.fontMetric[font].height;
			checkScrollBounds();
		}
	}

	public void keyPress() {
		int key = Keyboard.getEventKey();
		if(key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT) {
			isShiftDown = true;
		} else if(key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL) {
			isCtrlDown = true;
		}
	}

	public void keyRelease() {
		int key = Keyboard.getEventKey();
		if(key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT) {
			isShiftDown = false;
		} else if(key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL) {
			isCtrlDown = false;
		}
	}

	public void focus() {
		focused = true;
	}

	public void unfocus() {
		focused = false;
	}
}
