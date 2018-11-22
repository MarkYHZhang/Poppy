import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.Stack;

public class MouseHelper {

	public static Stack<Point> offset = new Stack<>();

	static {
		offset.push(new Point(0, 0));
	}

	public static int grabX;
	public static int grabY;

	public static int height;

	public static void pushOffset(int x, int y) {
		offset.push(new Point(x, y));
	}

	public static void popOffset() {
		offset.pop();
	}

	public static int getX() {
		int x;
		if(Mouse.isGrabbed()) {
			x = grabX;
		} else {
			x = Mouse.getX();
		}
		return x - offset.peek().x;
	}
	public static int getY() {
		int y;
		if(Mouse.isGrabbed()) {
			y = grabY;
		} else {
			y = Mouse.getY();
		}
		return height - y - offset.peek().y;
	}
	public static int getDX() {
		return Mouse.getDX();
	}
	public static int getDY() {
		return -Mouse.getDY();
	}
	public static int getEventDX() {
		return Mouse.getEventDX();
	}
	public static int getEventDY() {
		return -Mouse.getEventDY();
	}
	public static int getEventX() {
		int x;
		if(Mouse.isGrabbed()) {
			x = grabX;
		} else {
			x = Mouse.getEventX();
		}
		return x - offset.peek().x;
	}
	public static int getEventY() {
		int y;
		if(Mouse.isGrabbed()) {
			y = grabY;
		} else {
			y = Mouse.getEventY();
		}
		return height - y - offset.peek().y;
	}

	public static void setCursorPosition(int x, int y) {
		Mouse.setCursorPosition(x + offset.peek().x, height - y - offset.peek().y);
	}

	public static void setGrabbed(boolean grabbed) {
		if(grabbed && !Mouse.isGrabbed()) {
			grabX = Mouse.getX();
			grabY = Mouse.getY();
			Mouse.setGrabbed(true);
		} else if(!grabbed){
			Mouse.setGrabbed(false);
		}
	}
}
