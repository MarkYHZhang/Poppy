package robot;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import main.Poppy;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.net.URI;
import java.util.ArrayDeque;

public class Robot {

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


        Server server = new Server(9999);
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(PythonCommunication.class);
            }
        };
        ContextHandler context = new ContextHandler();
        context.setContextPath("/socket");
        context.setHandler(wsHandler);
        server.setHandler(context);
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void executeCode(){
        new Thread(() -> {
            try {
                String code = Unirest.get(Poppy.SERVER_GET_CODE_URL).asString().getBody();
                String[] cmds = code.split("\\|");
                ArrayDeque<String> d = new ArrayDeque<String>();
                for (String cmd : cmds) {
                    if (cmd.equals("move,1")){
                        for (int i = 0; i < 8; i++)
                            d.add("move,0.2");
                    }else if(cmd.equals("move,-1")){
                        for (int i = 0; i < 8; i++)
                            d.add("move,-0.2");
                    }else {
                        d.add(cmd);
                    }
                }
                for (String raw : d) {
                    String[] args = raw.split(",");
                    String cmd = args[0];
                    double val = Double.parseDouble(args[1]);
                    if (cmd.equals("move")){
                        JackyCoolLib.move(val);
                    }else if (cmd.equals("turn")){
                        JackyCoolLib.turn(val);
                    }
                    if (cmd.equals("move")) Thread.sleep(200);
                    else Thread.sleep(500);
                }
            } catch (InterruptedException | UnirestException e) {
                e.printStackTrace();
            }
        }).start();
    }
//    private void vision(){
//        try {
//            VideoCapture vc = new VideoCapture( 320, 240);
//            List<DetectedFace> faces = null;
//            for (int i = 0; i < 10000; i++) {
//                faces = detector.detectFaces(Transforms.calculateIntensity(vc.getNextFrame()));
//                System.out.println(faces.get(0).getBounds());
//            }
//            vc.close();
////                System.out.println(faces.get(1).getBounds());
//        } catch (VideoCaptureException e) {
//            e.printStackTrace();
//        }
//
//    }

}
