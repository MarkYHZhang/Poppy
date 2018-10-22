package clientgui;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ControlPanel extends JPanel {


    private boolean[] buttons;

    public ControlPanel(){


        setLayout(new BorderLayout());

        JButton connect = new JButton("connect");
        connect.addActionListener(e -> {
            grabFocus();
        });

        add(connect, BorderLayout.NORTH);

        JLabel indicator = new JLabel("Waiting for Action", SwingConstants.CENTER);
        indicator.setFont(new Font("Serif", Font.PLAIN, 50));

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e);
                if (e.getKeyCode()==KeyEvent.VK_W){
                    indicator.setText("Moving Forwards");
                } else if (e.getKeyCode()==KeyEvent.VK_S){
                    indicator.setText("Moving Backwards");
                }else if (e.getKeyCode()==KeyEvent.VK_A){
                    indicator.setText("Turning Left");
                } else if (e.getKeyCode()==KeyEvent.VK_D){
                    indicator.setText("Turning Right");
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                indicator.setText("Waiting for Action");
                if (e.getKeyCode()==KeyEvent.VK_A){

                } else if (e.getKeyCode()==KeyEvent.VK_D){

                } else if (e.getKeyCode()==KeyEvent.VK_W){

                } else if (e.getKeyCode()==KeyEvent.VK_S){

                }
            }
        });
        setFocusable(true);

        add(indicator, BorderLayout.CENTER);

    }

}
