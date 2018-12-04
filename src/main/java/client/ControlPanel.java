package client;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import main.Poppy;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import robot.JackyCoolLib;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URI;
import java.util.ArrayDeque;

public class ControlPanel extends JPanel {

    private ClientSocket socket;

    public ControlPanel(){

        setLayout(new BorderLayout());

        JButton connect = new JButton("connect");
        connect.addActionListener(e -> {
            grabFocus();

            WebSocketClient client = new WebSocketClient();
            socket = new ClientSocket();

            PeriodicSend ps = new PeriodicSend(socket);
            new Thread(ps).start();

            try {
                client.start();

//                URI echoUri = new URI(Poppy.SERVER_SOCKET_URL);
                URI echoUri = new URI(Poppy.SERVER_SOCKET_URL);
                ClientUpgradeRequest request = new ClientUpgradeRequest();
                client.connect(socket,echoUri,request);
                System.out.println("Connecting...");
            }
            catch (Throwable t) {
                t.printStackTrace();
            }

        });

        add(connect, BorderLayout.NORTH);

        JLabel indicator = new JLabel("Waiting for Action", SwingConstants.CENTER);
        indicator.setFont(new Font("Serif", Font.PLAIN, 50));

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_W){
                    indicator.setText("Moving Forwards");
                    socket.setUp(true);
//                    socket.moveForwards();
                } else if (e.getKeyCode()==KeyEvent.VK_S){
                    indicator.setText("Moving Backwards");
                    socket.setDown(true);
//                    socket.moveBackwards();
                }else if (e.getKeyCode()==KeyEvent.VK_A){
                    indicator.setText("Turning Left");
                    socket.setLeft(true);
//                    socket.turn(10);
                } else if (e.getKeyCode()==KeyEvent.VK_D){
                    indicator.setText("Turning Right");
                    socket.setRight(true);
//                    socket.turn(-10);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                indicator.setText("Waiting for Action");
                if (e.getKeyCode()==KeyEvent.VK_W){
//                    indicator.setText("Moving Forwards");
                    socket.setUp(false);
                } else if (e.getKeyCode()==KeyEvent.VK_S){
//                    indicator.setText("Moving Backwards");
                    socket.setDown(false);
                }else if (e.getKeyCode()==KeyEvent.VK_A){
//                    indicator.setText("Turning Left");
                    socket.setLeft(false);
                } else if (e.getKeyCode()==KeyEvent.VK_D){
//                    indicator.setText("Turning Right");
                    socket.setRight(false);
                }
            }
        };

        addKeyListener(keyListener);
        setFocusable(true);

        add(indicator, BorderLayout.CENTER);

    }

}
