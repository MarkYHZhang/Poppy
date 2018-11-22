package com.markyhzhang.poppyandroid;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class MainActivity extends AppCompatActivity {

    private OkHttpClient client;
    private Request request = new Request.Builder().url("ws://40.117.127.142:9001/socket").build();
    private ConnectionListener connection;
    private WebSocket ws;
    private TextView tv;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.text_status);
        client = new OkHttpClient();
        snackbar = Snackbar.make(findViewById(R.id.main_layout), "",
                Snackbar.LENGTH_SHORT);
        connection = new ConnectionListener(tv, snackbar);
    }

    public void performAction(View view){
        String t = ((TextView)view).getText().toString();
        System.out.println(t);
        if (t.equals(getResources().getString(R.string.connect))) {
            ws = client.newWebSocket(request, connection);
            System.out.println("FUCK");
            tv.setText("Status: Connecting...");
        } else if (t.equals(getResources().getString(R.string.disconnect))) {
            ws.close(1000, "bye");
            tv.setText("Status: Disconnected");
        }
        if (connection.isConnected()) {
            if (t.equals(getResources().getString(R.string.forward))) {
                connection.moveForwards();
            } else if (t.equals(getResources().getString(R.string.backward))) {
                connection.moveBackwards();
            } else if (t.equals(getResources().getString(R.string.left))) {
                connection.turn(10);
            } else if (t.equals(getResources().getString(R.string.right))) {
                connection.turn(-10);
            }
        }else{
            showSnackbar("Not connected to server!");
        }
    }

    public void showSnackbar(String msg){
        snackbar.setText(msg).show();
    }
}
