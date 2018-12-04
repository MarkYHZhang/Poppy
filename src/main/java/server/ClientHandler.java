package server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

@WebSocket
public class ClientHandler {

    private Session robot;
    private int stepDist = 1;

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        System.out.println("connected user " + user);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {

    }

    private final static double revDist = 0.2;
    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        if (message.equals("robotInit")){
            System.out.println("NICE!");
            robot = user;
            return;
        }
        //turn and move
        if(message.startsWith("cmd ")){
            try {
                String cmd = message.split(" ")[1];
                if (cmd.equals("moveForwards"))
                    robot.getRemote().sendString("move,"+revDist);
                else if (cmd.equals("moveBackwards"))
                    robot.getRemote().sendString("move,"+revDist*-1);
                else if (cmd.equals("move"))
                    robot.getRemote().sendString("move,"+message.split(" ")[2]);
                else if(cmd.equals("turn"))
                    robot.getRemote().sendString("turn,"+message.split(" ")[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendUpdateCodeQuery(){
        try{
            robot.getRemote().sendString("getCode");
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
