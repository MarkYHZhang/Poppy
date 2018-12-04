import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

public class GuiContainer extends GuiComponent {

	public ArrayList<GuiComponent> components = new ArrayList<>();
	public int focusInd = -1;
	public boolean mouseDown;
	public Layout layout;
	public GuiComponent mouseOver;

	public GuiContainer(Layout layout) {
		this.layout = layout;
	}

	public void resize() {
		if(layout != null) {
			layout.layout(this);
		}
		for(GuiComponent c : components) {
			c.resize();
		}
	}

	public GuiComponent getComponentUnder(int x, int y) {
		int ind = getComponentIndUnder(x, y);
		if(ind >= 0) {
			return components.get(ind);
		}
		return null;
	}

	public int getComponentIndUnder(int x, int y) {
		for(int i = components.size() - 1; i >= 0; --i) {
			GuiComponent elem = components.get(i);
			if(elem.x <= x && x < elem.x + elem.width && elem.y <= y && y < elem.y + elem.height) {
				return i;
			}
		}
		return -1;
	}

	public void add(GuiComponent c) {
		components.add(c);
	}

	public void render() {
		for(int i = 0; i < components.size(); ++i) {
			GuiComponent c = components.get(i);
			GLUtil.pushRegion(c.x, c.y, c.width, c.height);
			c.render();
			GLUtil.popRegion();
		}
	}

	public void mousePress(int x, int y, int button) {
		if(focusInd != -1) {
			components.get(focusInd).unfocus();
		}
		focusInd = getComponentIndUnder(x, y);
		mouseDown = true;
		if(focusInd != -1) {
			GuiComponent c = components.get(focusInd);
			c.mousePress(x - c.x, y - c.y, button);
			c.focus();
		}
	}

	public void mouseRelease(int x, int y, int button) {
		mouseDown = false;
		if(focusInd != -1) {
			GuiComponent c = components.get(focusInd);
			c.mouseRelease(x - c.x, y - c.y, button);
		}
		GuiComponent c = getComponentUnder(x, y);
		if(c != mouseOver) {
			if(mouseOver != null) {
				mouseOver.mouseLeave();
				mouseOver = null;
			}
		}
	}

	public void mouseMove(int x, int y) {
		if(mouseDown) {
			if(focusInd != -1) {
				GuiComponent c = components.get(focusInd);
				c.mouseMove(x - c.x, y - c.y);
			}
		} else {
			GuiComponent c = getComponentUnder(x, y);
			if (c != null) {
				c.mouseMove(x - c.x, y - c.y);
			}
			if(mouseOver != c) {
				if(mouseOver != null) {
					mouseOver.mouseLeave();
				}
				mouseOver = c;
			}
		}
	}

	public void mouseWheel(int x, int y, int wheel) {
		GuiComponent c = getComponentUnder(x, y);
		if(c != null) {
			c.mouseWheel(x - c.x, y - c.y, wheel);
		}
	}

	public void mouseLeave() {
		if(!mouseDown) {
			if (mouseOver != null) {
				mouseOver.mouseLeave();
				mouseOver = null;
			}
		}
	}

	public void keyPress() {
		if(focusInd != -1) {
			components.get(focusInd).keyPress();
		}
	}

	public void keyRelease() {
		if(focusInd != -1) {
			components.get(focusInd).keyRelease();
		}
	}

	public void keyType() {
		if(focusInd != -1) {
			components.get(focusInd).keyType();
		}
	}

	public void input(double dt) {
		if(focusInd != -1) {
			components.get(focusInd).input(dt);
		}
	}

	public void unfocus() {
		if(focusInd != -1) {
			components.get(focusInd).unfocus();
		}
	}

	public interface Layout {
		void layout(GuiContainer c);
	}
}
