package client;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class TerminalPanel extends JPanel {

    public TerminalPanel(){
        setLayout(new BorderLayout());

        JTextArea terminalDisplay = new JTextArea();
        terminalDisplay.setEditable(false);

        terminalDisplay.setFont(new Font(Font.MONOSPACED,Font.PLAIN, 15));

        JScrollPane scrollPane = new JScrollPane(terminalDisplay);

        JTextField input = new JTextField();
        input.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER) {
                    if (!input.getText().trim().equals("")) {
                        terminalDisplay.append("you@main.Poppy: " + input.getText() + "\n");
                        input.setText("");
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });

        add(scrollPane, BorderLayout.CENTER);
        add(input, BorderLayout.SOUTH);

    }

}
