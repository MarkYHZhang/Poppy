import java.util.Scanner;

public class MonitorDt extends Monitor {
	public static double dt;
	public static double time;
	public void onData(String str) {
		Scanner scanner = new Scanner(str);
		dt = scanner.nextDouble();
		time = scanner.nextDouble();
	}
	public String text() {
		return Math.round(1.0 / dt) + " Hz, " + Math.round(time * 1e6) + " us";
	}
}
