package main;

import client.MainFrame;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import server.ClientHandler;
import spark.Spark;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.Arrays;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.webSocket;


public class Poppy {

    static String code;

    public static final String SERVERIP = "0.0.0.0";
    public static final short SERVER_PORT = 9001;
    public static final String SERVER_UPDATE_CODE_URL = "http://"+SERVERIP+":"+SERVER_PORT+"/updateCode";
    public static final String SERVER_GET_CODE_URL = "http://"+SERVERIP+":"+SERVER_PORT+"/getCode";
    public static final String SERVER_SOCKET_URL = "ws://"+SERVERIP+":"+SERVER_PORT+"/socket";

    private static final HaarCascadeDetector detector = new HaarCascadeDetector();

    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("server")){

            Spark.exception(Exception.class, (exception, request, response) -> {
                exception.printStackTrace();
            });


            port(SERVER_PORT);

            webSocket("/socket", server.ClientHandler.class);


            get("/getCode", (request, response) -> {
                System.out.println("FUCK");
                return code;
            });

            post("/updateCode", (request, response) -> {
                System.out.println("FUCK");
                System.out.println(request.body());
                code = compileCode(request.body());
                System.out.println(code);
                return "Received";
            });


            init();

        }else if(args.length==1&&args[0].equals("robot")){

            try {
                System.out.println("v1");
                VideoCapture vc = new VideoCapture( 320, 240);
                List<DetectedFace> faces = null;
                for (int i = 0; i < 10000; i++) {
                    faces = detector.detectFaces(Transforms.calculateIntensity(vc.getNextFrame()));
                    System.out.println(faces.get(0).getBounds());
                }
                vc.close();
//                System.out.println(faces.get(1).getBounds());
            } catch (VideoCaptureException  e) {
                e.printStackTrace();
            }


        }else{//client end
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            new MainFrame();
        }

    }

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

    private static String compileCode(String code){
        StringBuffer sb = new StringBuffer();
        String[] rawCode = code.split(";");
        System.out.println(Arrays.toString(rawCode));
        int rev = 1;
        for (String line : rawCode) {
            line = line.trim();
            if (line.equals("turnLeft()")) line="turn(90)";
            else if (line.startsWith("turnRight()")) line="turn(270)";
            else if (line.startsWith("forward()")) line="move(0)";
            else if (line.startsWith("backward()")) line="move(1)";

            if (line.startsWith("setStepDist(")){
                try {
                    rev = Integer.parseInt(line.substring(12, line.length()-1).trim());
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }else if(line.startsWith("move(")){
                try {
                    int direction = Integer.parseInt(line.substring(5, line.length()-1).trim());
                    sb.append("move,"+direction+","+rev);
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }else if(line.startsWith("turn(")){
                try {
                    int degree = Integer.parseInt(line.substring(5, line.length()-1).trim());
                    sb.append("turn,"+degree);
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }else if(line.startsWith("activateAssistant()")) sb.append("assistant");
            else if(line.startsWith("moveToPerson()")) sb.append("person");
            else if(line.startsWith("say(")){
                try {
                    String words = line.substring(4, line.length()-1);
                    sb.append("say,"+words);
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }else if(line.startsWith("loop(")){
                try{
                    String[] args = line.substring(5,line.length()-1).trim().split(",");
                    int iterations = Integer.parseInt(args[0]);
                    String instruction = compileCode(args[1].replace("+",";").trim());
                    StringBuffer looped = new StringBuffer(instruction);
                    for (int i = 1; i < iterations; i++) {
                        looped.append(instruction);
                    }
                    sb.append(looped.substring(0,looped.length()-1));
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }
            sb.append("|");
        }
        return sb.toString();
    }

}