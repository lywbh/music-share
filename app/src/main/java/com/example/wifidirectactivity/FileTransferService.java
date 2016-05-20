package com.example.wifidirectactivity;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            ArrayList<String> fileUriList = intent.getStringArrayListExtra(EXTRAS_FILE_PATH);
            String host = intent.getStringExtra(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            try {
                for (String fileUri : fileUriList) {
                    Socket socket = new Socket();
                    Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                    Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());

                    OutputStream os = socket.getOutputStream();
                    InputStream is = context.getContentResolver().openInputStream(Uri.parse("file://" + fileUri));
                    showToast("Uploading " + (new File(fileUri)).getName() + " ...");
                    Log.d(WiFiDirectActivity.TAG, "Client: Sending files");
                    DeviceDetailFragment.copyFile(is, os);
                    Log.d(WiFiDirectActivity.TAG, "Client: Sending completed");

                    /* 不知道为什么，这里必须停一下，不然会报错
                     * 可能是要等待客户端把流里的东西都读完，才能关闭socket
                     */
                    Thread.sleep(1000);

                    socket.close();
                }
                showToast("Upload completed");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void showToast(String message) {
        final String msg = message;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
