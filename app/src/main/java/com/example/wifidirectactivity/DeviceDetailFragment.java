package com.example.wifidirectactivity;

/*
 * Copyright (C)  The Android Open Source Project
 *
 * Licensed under the Apache License, Version . (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-.
 *
 * Unless required by aP2plicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wifidirectactivity.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements
        ConnectionInfoListener {

    //    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    BroadcastReceiver serviceReceiver = null;

    ArrayList<String> audioList;
    ArrayList<String> audioNameList;
    ArrayList<String> audioMd5List;
    ArrayList<String> myMd5List;

    ArrayList<Integer> selectedAudioList = new ArrayList<Integer>();
    ArrayList<String> selectedAudioNameList = new ArrayList<String>();

    private ServerSocket serverNameSocket;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Intent intent = getActivity().getIntent();
        audioList = intent.getStringArrayListExtra("audioList");
        audioNameList = intent.getStringArrayListExtra("nameList");
        audioMd5List = intent.getStringArrayListExtra("md5List");
        myMd5List = intent.getStringArrayListExtra("myMd5List");

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        config.wps.setup = WpsInfo.PBC;
                        config.groupOwnerIntent = 15;
                        // 这个参数（0-15）用于建议GroupOwner是谁，即调整主动连接方成为Server的概率

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        progressDialog = ProgressDialog.show(getActivity(),
                                "Press back to cancel",
                                "Connecting to :" + device.deviceAddress, true, true
                        );

                        ((DeviceActionListener) getActivity()).connect(config);
                    }
                });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        sendAudioNameList();
                    }
                });

        serviceReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                ArrayList<Integer> selectedResult = intent.getIntegerArrayListExtra("selectedAudioList");
                ArrayList<String> fileToSend = new ArrayList<String>();
                for (int i : selectedResult) {
                    fileToSend.add(audioList.get(i));
                }
                sendAudioFile(fileToSend);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("SelectAudios");
        getActivity().registerReceiver(serviceReceiver, filter);

        return mContentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(serviceReceiver);
        try {
            if (serverNameSocket != null) serverNameSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAudioNameList() {
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending Audio List");
        Log.d(WiFiDirectActivity.TAG, "Intent-----------NameList");
        Intent serviceIntent = new Intent(getActivity(), ArrayTransferService.class);
        serviceIntent.setAction(ArrayTransferService.ACTION_SEND_ARRAY);
        serviceIntent.putExtra(ArrayTransferService.EXTRAS_ARRAY, audioNameList);
        serviceIntent.putExtra(ArrayTransferService.EXTRAS_MD5, audioMd5List);
        serviceIntent.putExtra(ArrayTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(ArrayTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }

    public void sendAudioFile(ArrayList<String> filePath) {
        ArrayList<String> uriList = new ArrayList<String>();
        for (String item : filePath) {
            uriList.add(Uri.parse(item).toString());
        }
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uriList);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uriList);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uriList);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8989);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text) + (info.isGroupOwner ? getResources().getString(
                R.string.yes) : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            new TitleServerAsyncTask(getActivity(), mContentView.findViewById(R.id.select_status_text)).execute();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.client_text));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView
                .findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(
                View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * Receive song names and perform the select menu
     */
    public class TitleServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        private String[] nItems;
        ArrayList<String> recvMd5List;

        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        /**
         * @param context    context
         * @param statusText statusText
         */
        public TitleServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                serverNameSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket nameClient = serverNameSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");

                ois = new ObjectInputStream(nameClient.getInputStream());
                oos = new ObjectOutputStream(nameClient.getOutputStream());
                ArrayList<String> nItemsArray = (ArrayList<String>) ois.readObject();
                recvMd5List = (ArrayList<String>) ois.readObject();
                if (nItemsArray == null || nItemsArray.isEmpty()) return "empty";
                nItems = nItemsArray.toArray(new String[nItemsArray.size()]);
                return "success";
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return "fail";
        }

        @Override
        protected void onPostExecute(String result) {
            if ("fail".equals(result)) return;
            if ("empty".equals(result)) {
                new TitleServerAsyncTask(getActivity(), mContentView.findViewById(R.id.select_status_text)).execute();
                return;
            }
            selectedAudioList.clear();
            selectedAudioNameList.clear();
            final boolean[] nSelect = new boolean[nItems.length];
            for (boolean item : nSelect) item = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle("多项选择");
            builder.setMultiChoiceItems(nItems, nSelect,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1, boolean arg2) {
                            if (arg2) {
                                if (myMd5List.contains(recvMd5List.get(arg1))) {
                                    Toast.makeText(getActivity(),
                                            "该音乐已存在！",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                                selectedAudioList.add(arg1);
                                selectedAudioNameList.add(nItems[arg1]);
                            } else {
                                selectedAudioList.remove((Integer) arg1);
                                selectedAudioNameList.remove(nItems[arg1]);
                            }
                        }
                    }
            );
            // 设置确定按钮
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    try {
                        // 选择列表关闭，发送选中的文件编号
                        oos.writeObject(selectedAudioList);
                        oos.flush();
                        serverNameSocket.close();
                        if (selectedAudioList.isEmpty()) {
                            new TitleServerAsyncTask(getActivity(), mContentView.findViewById(R.id.select_status_text)).execute();
                        } else {
                            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
                        }
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
            });
            // 设置取消按钮
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    try {
                        // 选择列表关闭，发送空列表
                        selectedAudioList.clear();
                        selectedAudioNameList.clear();
                        oos.writeObject(selectedAudioList);
                        oos.flush();
                        serverNameSocket.close();
                        new TitleServerAsyncTask(getActivity(), mContentView.findViewById(R.id.select_status_text)).execute();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
            });
            // 显示多选框
            builder.create().show();
        }

        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a file name receiver");
        }
    }

    /**
     * A simple server socket that accepts connection and writes some data on the stream.
     */
    public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context    context
         * @param statusText statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                for (String fileName : selectedAudioNameList) {
                    ServerSocket serverSocket = new ServerSocket(8989);
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    Socket client = serverSocket.accept();
                    Log.d(WiFiDirectActivity.TAG, "Server: Connection done");

                    final File f = new File(Environment.getExternalStorageDirectory()
                            + "/" + context.getPackageName()
                            + "/" + System.currentTimeMillis()
                            + "-" + fileName
                    );
                    File dirs = new File(f.getParent());
                    if (!dirs.exists()) dirs.mkdirs();
                    f.createNewFile();
                    InputStream is = client.getInputStream();
                    OutputStream os = new FileOutputStream(f);
                    Log.d(WiFiDirectActivity.TAG, "Server: Downloading " + f.toString());
                    copyFile(is, os);
                    Log.d(WiFiDirectActivity.TAG, "Server: Download completed " + f.toString());

                    serverSocket.close();
                }
                return "success";
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            }
            return "fail";
        }

        @Override
        protected void onPostExecute(String result) {
            getFiles(Environment.getExternalStorageDirectory().getPath());
            new TitleServerAsyncTask(getActivity(), mContentView.findViewById(R.id.select_status_text)).execute();
            if ("success".equals(result)) {
                Toast.makeText(getActivity(), "下载完成", Toast.LENGTH_SHORT).show();
            } else if ("fail".equals(result)) {
                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(getActivity(), "开始下载 ...", Toast.LENGTH_SHORT).show();
            statusText.setText("Opening a file receiver");
        }
    }

    public static boolean copyFile(InputStream in, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.getMessage());
            return false;
        }
        return true;
    }

    private void getFiles(String url) {
        File dir = new File(url); // 创建文件对象
        File[] files = dir.listFiles();
        try {
            for (File f : files) { // 通过for循环遍历获取到的文件数组
                if (f.isDirectory()) { // 如果是目录，也就是文件夹
                    getFiles(f.getAbsolutePath()); // 递归调用
                } else {
                    if (isAudioFile(f.getPath())) { // 如果是音频文件
                        myMd5List.add(MainActivity.getFileMD5(f)); // 将文件名添加到list集合中
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // 输出异常信息
        }
    }

    private static String[] imageFormatSet = new String[]{"mp3", "wav", "3gp"}; // 合法的音频文件格式

    // 判断是否为音频文件
    private static boolean isAudioFile(String path) {
        for (String format : imageFormatSet) { // 遍历数组
            if (path.contains(format)) { // 判断是否为有合法的音频文件
                return true;
            }
        }
        return false;
    }
}