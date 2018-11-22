package robot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class JackyCoolLib {

    private static boolean enablePipe = true;

    private static BufferedWriter bw;
    static {
        if (enablePipe) {
            try {
                bw = new BufferedWriter(new FileWriter("/tmp/poppypipe"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void move(double vec){
        sendpipe("move " + vec);
    }
    public static void turn(double degree){
        sendpipe("turn " + degree);
    }

    public static void sendpipe(String s){
        if (enablePipe) {
            try {
                bw.write(s+"\n");
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(s);
    }

}
