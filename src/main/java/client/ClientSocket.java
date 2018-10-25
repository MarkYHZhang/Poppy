package client;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

@WebSocket
public class ClientSocket {
    private Session server;

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        server = user;
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {}

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {}

    void moveForwards(){
        try {
            server.getRemote().sendString("cmd moveForwards");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void moveBackwards(){
        try {
            server.getRemote().sendString("cmd moveBackwards");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void turn(int degrees){
        try {
            server.getRemote().sendString("cmd turn " + degrees);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
