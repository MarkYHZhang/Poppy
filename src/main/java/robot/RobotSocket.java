package robot;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

@WebSocket
public class RobotSocket {

    private Robot instance;

    public RobotSocket(Robot robot){
        instance = robot;
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        System.out.println("Connects!!");
        try{
            user.getRemote().sendString("robotInit");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {}


    /*
    *   turn turn(angle from 0 to 360 with north being 0, and west being 90)
        move move(1 forward or -1 backwards, dist in rev)
        assistant activateAssistant()
        person moveToPerson() call python to move to person
        say say(words here) //call microsoft API then send the download URL link of the wav sound
    **/
    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        String[] args = message.split(",");
        String cmd = args[0];
        if(cmd.equals("getCode")){
            instance.executeCode();
        }else{
            double param = Double.parseDouble(args[1]);
            if (cmd.equals("move")){
                JackyCoolLib.move(param);
            }else if (cmd.equals("turn")){
                JackyCoolLib.turn(param);
            }
        }
    }

}
