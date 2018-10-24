package client;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

public class MainFrame extends JFrame {


    public MainFrame(){

        setTitle("main.Poppy Client Terminal");

        setSize(800,500);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        TerminalPanel panel1 = new TerminalPanel();
        tabbedPane.addTab("Terminal", null, panel1,
                "Interact with main.Poppy using commands");

        CodePanel panel2 = new CodePanel();
        tabbedPane.addTab("Code", null, panel2,
                "Program main.Poppy to do extraordinary things!");

        ControlPanel panel3 = new ControlPanel();
        tabbedPane.addTab("Control", null, panel3,
                "Control main.Poppy using keyboard");

        add(tabbedPane);

        setVisible(true);
    }


}
