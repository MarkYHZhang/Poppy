package client;

public class PeriodicSend implements Runnable{

    private ClientSocket socket;

    public PeriodicSend(ClientSocket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        while (true) {
            if (socket.isUp()) {
                socket.moveForwards();
                System.out.println("moving forward");
            }
            if (socket.isDown()) {
                socket.moveBackwards();
                System.out.println("moving backward");
            }
            if (socket.isLeft()) {
                System.out.println("turning left");
                socket.turn(5);
            }
            if (socket.isRight()) {
                System.out.println("turning right");
                socket.turn(-5);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
