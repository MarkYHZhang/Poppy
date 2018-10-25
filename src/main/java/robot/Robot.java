package robot;

import client.ClientSocket;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import main.Poppy;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

import java.net.URI;
import java.util.List;

public class Robot {

    private static final HaarCascadeDetector detector = new HaarCascadeDetector();

    public Robot(){
        WebSocketClient client = new WebSocketClient();
        RobotSocket socket = new RobotSocket(this);
        try {
            client.start();

            URI echoUri = new URI(Poppy.SERVER_SOCKET_URL);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket,echoUri,request);
            System.out.println("Connecting...");
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    void executeCode(){
        try {
            System.out.println(Unirest.get("http://0.0.0.0:9001/getCode").asString().getBody());
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    private void vision(){
        try {
            VideoCapture vc = new VideoCapture( 320, 240);
            List<DetectedFace> faces = null;
            for (int i = 0; i < 10000; i++) {
                faces = detector.detectFaces(Transforms.calculateIntensity(vc.getNextFrame()));
                System.out.println(faces.get(0).getBounds());
            }
            vc.close();
//                System.out.println(faces.get(1).getBounds());
        } catch (VideoCaptureException e) {
            e.printStackTrace();
        }

    }

}
