package com.example.wifidirectactivity;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ArrayTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_ARRAY = "com.example.android.wifidirect.SEND_ARRAY";
    public static final String EXTRAS_ARRAY = "array_content";
    public static final String EXTRAS_MD5 = "md5_content";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public ArrayTransferService(String name) {
        super(name);
    }

    public ArrayTransferService() {
        super("ArrayTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_SEND_ARRAY)) {
            ArrayList<String> array = intent.getStringArrayListExtra(EXTRAS_ARRAY);
            ArrayList<String> md5 = intent.getStringArrayListExtra(EXTRAS_MD5);
            String host = intent.getStringExtra(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                oos.writeObject(array);
                oos.writeObject(md5);
                oos.flush();
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
                ArrayList<Integer> selectedAudioList = (ArrayList<Integer>) ois.readObject();
                if (!selectedAudioList.isEmpty()) {
                    Intent response = new Intent();
                    response.setAction("SelectAudios");
                    response.putExtra("selectedAudioList", selectedAudioList);
                    sendBroadcast(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
