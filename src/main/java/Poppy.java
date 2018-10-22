import clientgui.MainFrame;
import com.github.sarxos.webcam.Webcam;
import io.indico.Indico;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.post;


public class Poppy {

    static String code;



    private static final long serialVersionUID = 1L;
    private static final HaarCascadeDetector detector = new HaarCascadeDetector();

    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("server")){
            get("/getCode", (request, response) -> {
                return code;
            });

            post("/updateCode", (request, response) -> {
                code = compileCode(request.params("content"));
                return "Received";
            });
        }else if(args.length==1&&args[0].equals("robot")){

            Indico indico = null;
//            try {
//                indico = new Indico("627079b9b49deba248916674bb95cd7d");

                Webcam webcam = Webcam.getDefault();
                webcam.setViewSize(new Dimension(640, 480));
                webcam.open();
                BufferedImage bi = webcam.getImage();
//                ImageIO.write(bi, "PNG", new File("img.png"));
                webcam.close();

                List<DetectedFace> faces = null;
                faces = detector.detectFaces(ImageUtilities.createFImage(bi));

                System.out.println(faces.get(0).getBounds());
//                IndicoResult single = indico.facialLocalization.predict(bi);
//                System.out.println(single.getFacialLocalization());
//            } catch (IndicoException e) {
//                e.printStackTrace();
//            }

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

        A turn(angle from 0 to 360 with north being 0, and west being 90)
        B move(1 forward or -1 backwards, dist in rev)
        C activateAssistant()
        D moveToPerson() call python to move to person
        E say(words here) //call microsoft API then send the download URL link of the wav sound



        setStepDist(in revolutions)

        setSafeMode(true/false)

        loop(5,forward()+)
    */

    private static String compileCode(String code){
        StringBuffer sb = new StringBuffer("");
        String[] rawCode = code.split(";");
        int rev = 1;
        for (String line : rawCode) {
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
                    sb.append("B"+direction+rev);
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }else if(line.startsWith("turn(")){
                try {
                    int degree = Integer.parseInt(line.substring(5, line.length()-1).trim());
                    sb.append("A"+degree);
                }catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
            }else if(line.startsWith("activateAssistant()")) sb.append("C");
            else if(line.startsWith("moveToPerson()")) sb.append("D");
            else if(line.startsWith("say(")){
                try {
                    String words = line.substring(4, line.length()-1);
                    sb.append("E"+words);
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
