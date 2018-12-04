import java.util.Scanner;

public class MonitorOrientation extends Monitor {

	public double tilt, heading;

	public void onData(String str) {
		Scanner scanner = new Scanner(str);
		tilt = scanner.nextDouble();
		heading = scanner.nextDouble();
	}

	public String text() {
		return String.format("T: %11.6f\nH: %11.6f", tilt, heading);
	}
}
