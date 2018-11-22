package com.markyhzhang.poppyandroid;

import android.support.design.widget.Snackbar;
import android.widget.TextView;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ConnectionListener extends WebSocketListener{

    private TextView tv;
    private WebSocket server;
    private boolean connected;
    private Snackbar snackbar;

    public boolean isConnected() {
        return connected;
    }

    public ConnectionListener(TextView tv, Snackbar snackbar){
        this.tv=tv;
        this.snackbar = snackbar;
    }

    private static final int NORMAL_CLOSURE_STATUS = 1000;
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        server = webSocket;
        tv.setText("Status: Connected");
        connected = true;
    }
    @Override
    public void onMessage(WebSocket webSocket, String text) { }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) { }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        connected = false;
    }
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        connected = false;
        tv.setText("Status: Disconnected");
        t.printStackTrace();
 //        snackbar.setText("Server connection lost!").show();

    }

    void moveForwards(){
        server.send("cmd moveForwards");
    }

    void moveBackwards(){
        server.send("cmd moveBackwards");
    }

    void turn(int degrees) {
        server.send("cmd turn " + degrees);
    }

}
