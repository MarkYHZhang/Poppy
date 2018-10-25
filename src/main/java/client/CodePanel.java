package client;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import main.Poppy;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.util.Scanner;

public class CodePanel extends JPanel{

    public CodePanel(){
        setLayout(new BorderLayout());
        JButton executeButton = new JButton("Execute");

        JTextArea codeArea = new JTextArea();
        codeArea.setFont(new Font(Font.MONOSPACED,Font.PLAIN, 15));
        JScrollPane scrollPane = new JScrollPane(codeArea);

        add(scrollPane, BorderLayout.CENTER);

        add(executeButton, BorderLayout.LINE_END);

        executeButton.addActionListener(e -> {

            StringBuffer sb = new StringBuffer();
            Scanner sc = new Scanner(codeArea.getText());
            while (sc.hasNext()){
                sb.append(sc.nextLine().trim());
                sb.append(";");
            }

            try {
                System.out.println(Unirest.post(Poppy.SERVER_UPDATE_CODE_URL).body(sb.toString()).asString());
            } catch (UnirestException ex) {
                ex.printStackTrace();
            }
        });
    }
}
