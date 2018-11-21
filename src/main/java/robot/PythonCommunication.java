package robot;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;


@WebSocket
public class PythonCommunication {

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {

    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {}


    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        String[] args = message.split(",");
        String cmd = args[0];
        double param = Double.parseDouble(args[1]);
        if (cmd.equals("move")) {
            JackyCoolLib.move(param);
        } else if (cmd.equals("turn")) {
            JackyCoolLib.turn(param);
        }
    }

}
