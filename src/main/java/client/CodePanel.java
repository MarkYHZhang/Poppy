package client;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import main.Poppy;
import robot.JackyCoolLib;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Scanner;

public class CodePanel extends JPanel{

    private JTextArea codeArea;

    public CodePanel(){
        setLayout(new BorderLayout());
        JButton executeButton = new JButton("Execute");
        JButton saveButton = new JButton("Save");
        JButton clearButton = new JButton("Clear");

        codeArea = new JTextArea();
        codeArea.setFont(new Font(Font.MONOSPACED,Font.PLAIN, 15));

        try {
            codeArea.addKeyListener(new KeyListener() {

                int pre = 10;

                Robot robot = new Robot();

                @Override
                public void keyTyped(KeyEvent e) {

                }

                @Override
                public void keyPressed(KeyEvent e) {

                }
                //left 37
                //right 39
                //enter 10

                @Override
                public void keyReleased(KeyEvent e) {
                    int t=pre;
                    pre=e.getKeyCode();
                    if(t!=10&&codeArea.getText().length()!=1){
                        return;
                    }
                    if (e.getKeyChar()=='f'){
                        codeArea.append("orward()");
                        robot.keyPress(10);
                        robot.keyRelease(10);
                    }else if (e.getKeyChar()=='b'){
                        codeArea.append("ackward()");
                        robot.keyPress(10);
                        robot.keyRelease(10);
                    }else if (e.getKeyChar()=='l'){
                        codeArea.append("eft()");
                        robot.keyPress(10);
                        robot.keyRelease(10);
                    }else if (e.getKeyChar()=='r'){
                        codeArea.append("ight()");
                        robot.keyPress(10);
                        robot.keyRelease(10);
                    }else if (e.getKeyChar()=='i'){
                        codeArea.append("ter(,)");
                        robot.keyPress(37);
                        robot.keyRelease(37);
                        robot.keyPress(37);
                        robot.keyRelease(37);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        JScrollPane scrollPane = new JScrollPane(codeArea);

        executeButton.addActionListener(e -> {
            try {
//                executeCode();
//                executeCode(compileCode((readCode().replace("\n","").replace("loop","iter").replace("turnLeft","left").replace("turnRight","right"))));
                Unirest.post(Poppy.SERVER_UPDATE_CODE_URL).body(readCode().replace("\n","").replace("loop","iter").replace("turnLeft","left").replace("turnRight","right")).asString();
            } catch (UnirestException ex) {
                ex.printStackTrace();
            }
        });

        saveButton.addActionListener(e -> {
            try {
                FileWriter fw = new FileWriter("script.poppy");
                fw.write(readCode());
                fw.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        clearButton.addActionListener(e -> {
            codeArea.setText("");
        });

        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel(new FlowLayout());
        buttonPane.add(executeButton);
        buttonPane.add(saveButton);
        buttonPane.add(clearButton);


            /*
    *

        forward()
        backward()
        turnLeft()
        turnRight()

        turn turn(angle from 0 to 360 with north being 0, and west being 90)
        move move(1 forward or -1 backwards, dist in rev)
        assistant activateAssistant()
        person moveToPerson() call python to move to person
        say say(words here) //call microsoft API then send the download URL link of the wav sound

        setStepDist(in revolutions)

        loop(5,forward()+)
    */

        JTextArea doc = new JTextArea(
                "Available commands: \n\n" +
                        "forward(): \n" +
                        "   move forward by one unit \n\n" +
                        "backward(): \n" +
                        "   move backward by one unit \n\n" +
                        "left(): \n" +
                        "   turn left 90 degrees \n\n" +
                        "right(): \n" +
                        "   turn right 90 degrees \n\n" +
                        "iter(numIter,X1()+X2()+...+Xn())\n" +
                        "   loop methods X1()+X2()+...+Xn()\n" +
                        "   in order for numIter time"
        );

        doc.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        doc.setEditable(false);

        JPanel rightPane = new JPanel(new BorderLayout());
        rightPane.add(buttonPane, BorderLayout.NORTH);
        rightPane.add(doc, BorderLayout.CENTER);

        File file = new File("script.poppy");
        if (file.exists()){
            System.out.println("yes");
            StringBuilder sb = new StringBuilder();
            Scanner sc = null;
            try {
                sc = new Scanner(file);
                while (sc.hasNext()){
                    String s = sc.nextLine().trim();
                    s = s.substring(0,s.length()-1);
                    sb.append(s);
                    sb.append("\n");
                }
                codeArea.setText(sb.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        add(rightPane, BorderLayout.LINE_END);

    }

    private String readCode(){
        StringBuilder sb = new StringBuilder();
        Scanner sc = new Scanner(codeArea.getText());
        while (sc.hasNext()){
            sb.append(sc.nextLine().trim());
            sb.append(";");
            sb.append("\n");
        }
        return sb.toString();
    }

//
//    void executeCode(String code){
//        new Thread(() -> {
//            try {
//                String[] cmds = code.split("\\|");
//                ArrayDeque<String> d = new ArrayDeque<String>();
//                for (String cmd : cmds) {
//                    if (cmd.equals("move,1")){
//                        for (int i = 0; i < 8; i++)
//                            d.add("move,0.2");
//                    }else if(cmd.equals("move,-1")){
//                        for (int i = 0; i < 8; i++)
//                            d.add("move,-0.2");
//                    }else {
//                        d.add(cmd);
//                    }
//                }
//                for (String raw : d) {
//                    String[] args = raw.split(",");
//                    String cmd = args[0];
//                    double val = Double.parseDouble(args[1]);
//                    if (cmd.equals("move")){
//                        JackyCoolLib.move(val);
//                    }else if (cmd.equals("turn")){
//                        JackyCoolLib.turn(val);
//                    }
//                    if (cmd.equals("move")) Thread.sleep(200);
//                    else Thread.sleep(3000);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//    private static String compileCode(String code){
//        StringBuffer sb = new StringBuffer();
//        String[] rawCode = code.split(";");
//        for (String line : rawCode) {
//            line = line.trim();
//            if (line.equals("left()")) line="turn(40)";
//            else if (line.startsWith("right()")) line="turn(-40)";
//            else if (line.startsWith("forward()")) line="move(1)";
//            else if (line.startsWith("backward()")) line="move(-1)";
//
//            System.out.println(line);
//
//            if(line.startsWith("move(")){
//                try {
//                    int direction = Integer.parseInt(line.substring(5, line.length()-1).trim());
//                    sb.append("move,"+(direction*1));
//                }catch (NumberFormatException nfe){
//                    nfe.printStackTrace();
//                }
//            }else if(line.startsWith("turn(")){
//                System.out.println("good");
//                try {
//                    int degree = Integer.parseInt(line.substring(5, line.length()-1).trim());
//                    sb.append("turn,"+degree);
//                }catch (NumberFormatException nfe){
//                    nfe.printStackTrace();
//                }
//            }else if(line.startsWith("iter(")){
//                try{
//                    String[] args = line.substring(5,line.length()-1).trim().split(",");
//                    int iterations = Integer.parseInt(args[0]);
//                    String instruction = compileCode(args[1].replace("+",";").trim());
//                    StringBuffer looped = new StringBuffer(instruction);
//                    for (int i = 1; i < iterations; i++) {
//                        looped.append(instruction);
//                    }
//                    sb.append(looped.substring(0,looped.length()-1));
//                }catch (NumberFormatException nfe){
//                    nfe.printStackTrace();
//                }
//            }
//            System.out.println();
//            sb.append("|");
//        }
//        return sb.toString();
//    }


}
