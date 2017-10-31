package com.example.ply.activitytest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import android.net.wifi.WifiManager;

import com.example.ply.activitytest.adapter.WifiListAdapter;
import com.example.ply.activitytest.application.ApplicationUtil;
import com.example.ply.activitytest.thread.ConnectThread;
import com.example.ply.activitytest.thread.ListenerThread;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.Manifest;



public class FirstActivity extends AppCompatActivity {
    private static final int FILE_CODE = 0;
    private SocketManager socketManager;

    private WifiManager wifiManager;
    private String chat_txt;
    private ListView listView;
    private Button btn_create_hostspot;
    private Button btn_send;
    private Button btn_close_hostspot;
    private Button btn_close_wifi;
    private Button btn_search;
    private Button btn_sendFile;
    private TextView nearbyWifiTV,connectWifiTV,textview,configurationWifiTV,wifiStateTV;
    private TextView text_state;
    private TextView textView;
    private EditText netIdET;
    private EditText messageEdit;
    private EditText txtEt;
    private WifiListAdapter wifiListAdapter;
    private WifiConfiguration config;
    private int wcgID;
    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "test";
    /**
     * 端口号
     */
    private static final int PORT = 54321;

    private static final int WIFICIPHER_NOPASS = 1;
    private static final int WIFICIPHER_WEP = 2;
    private static final int WIFICIPHER_WPA = 3;

    public static final int SEND_FILE=0;//传输文件
    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED = 2;//有设备连上热点
    public static final int SEND_MSG_SUCCSEE = 3;//发送消息成功
    public static final int SEND_MSG_ERROR = 4;//发送消息失败
    public static final int GET_MSG = 6;//获取新消息

    /**
     * 连接线程
     */
    private ConnectThread connectThread;

    /**
     * 监听线程
     */
    private ListenerThread listenerThread;


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_layout);

        wifiManager=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //nearbyWifiTV=(TextView)findViewById(R.id.nearby_wifi_content);

        initVIew();
        listenerThread = new ListenerThread(PORT, handler);
        listenerThread.start();
        socketManager = new SocketManager(handler);
        initBroadcastReceiver();
    }
    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(receiver, intentFilter);

    }


    private void initVIew(){
        listView = (ListView) findViewById(R.id.listView);
        btn_create_hostspot=(Button)findViewById(R.id.btn_create_hostspot);
        btn_close_hostspot=(Button)findViewById(R.id.btn_close_hostspot);
        btn_close_wifi=(Button)findViewById(R.id.btn_close_wifi);
        btn_send=(Button)findViewById(R.id.btn_send);
        btn_search=(Button)findViewById(R.id.btn_search);
        btn_sendFile=(Button)findViewById(R.id.btn_sendFile);
        text_state = (TextView) findViewById(R.id.text_state);
        textview=(TextView)findViewById(R.id.textview);
        txtEt = (EditText)findViewById(R.id.et);
        messageEdit=(EditText)findViewById(R.id.message);
        btn_close_wifi.setOnClickListener(hand);
        btn_create_hostspot.setOnClickListener(hand);
        btn_close_hostspot.setOnClickListener(hand);
        btn_send.setOnClickListener(hand);
        btn_search.setOnClickListener(hand);
        btn_sendFile.setOnClickListener(hand);


        wifiListAdapter = new WifiListAdapter(this, R.layout.second_layout);
        listView.setAdapter(wifiListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                wifiManager.disconnect();
                final ScanResult scanResult = wifiListAdapter.getItem(position);
                String capabilities = scanResult.capabilities;
                int type = WIFICIPHER_WPA;
                if (!TextUtils.isEmpty(capabilities)) {
                    if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                        type = WIFICIPHER_WPA;
                    } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                        type = WIFICIPHER_WEP;
                    } else {
                        type = WIFICIPHER_NOPASS;
                    }
                }
                config = isExsits(scanResult.SSID);
                if (config == null) {
                    if (type != WIFICIPHER_NOPASS) {//需要密码
                        final EditText editText = new EditText(FirstActivity.this);
                        final int finalType = type;

                        new AlertDialog.Builder(FirstActivity.this).setTitle("请输入Wifi密码").setIcon(
                                android.R.drawable.ic_dialog_info).setView(
                                editText).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.w("AAA", "editText.getText():" + editText.getText());
                                config = createWifiInfo(scanResult.SSID, editText.getText().toString(), finalType);
                                connect(config);
                            }
                        }).setNegativeButton("取消", null).show();
                        return;
                    } else {
                        config = createWifiInfo(scanResult.SSID, "", type);
                        connect(config);
                    }
                } else {
                    connect(config);
                }
            }
        });

    }


    private void connect(WifiConfiguration config) {
        text_state.setText("连接中...");
        wcgID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(wcgID, true);
    }

    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action =intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                Log.w("BBB","SCAN_RESULTS_AVAILABLE_ACTION");
               //wifi已经成功扫描到可用wifi
                List<ScanResult>scanResults=wifiManager.getScanResults();
                wifiListAdapter.clear();
                wifiListAdapter.addAll(scanResults);
            }else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                Log.w("BBB", "WifiManager.WIFI_STATE_CHANGED_ACTION");
                int wifiState=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,0);
                switch(wifiState){
                    case WifiManager.WIFI_STATE_ENABLED:
                        wifiManager.startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        //wifi关闭发出的广播
                        break;

                }
            }else if(action.equals(wifiManager.NETWORK_STATE_CHANGED_ACTION)){
                Log.w("BBB", "WifiManager.NETWORK_STATE_CHANGED_ACTION");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    text_state.setText("WIFI连接已断开");
                }else if (info.getState().equals(NetworkInfo.State.CONNECTED)){
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    text_state.setText("已连接到网络:" + wifiInfo.getSSID());
                    Log.w("AAA","wifiInfo.getSSID():"+wifiInfo.getSSID()+"  WIFI_HOTSPOT_SSID:"+WIFI_HOTSPOT_SSID);
                        if (wifiInfo.getSSID().equals("\"test\"")) {
                        //如果当前连接到的wifi是热点,则开启连接线程
                        Log.w("CCC","wifiInfo.getSSID():"+wifiInfo.getSSID()+"  WIFI_HOTSPOT_SSID:"+WIFI_HOTSPOT_SSID);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        ArrayList<String> connectedIP = getConnectedIP();
                                        for (String ip : connectedIP) {
                                            if (ip.contains(".")) {
                                                Log.w("AAA", "IP:" + ip);
                                                Socket socket = new Socket(ip, PORT);
                                                connectThread = new ConnectThread(socket, handler);
                                                connectThread.start();
                                            }
                                        }

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                    }
                } else {
                   NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == state.CONNECTING) {
                        text_state.setText("连接中...");
                    } else if (state == state.AUTHENTICATING) {
                        text_state.setText("正在验证身份信息...");
                    } else if (state == state.OBTAINING_IPADDR) {
                        text_state.setText("正在获取IP地址...");
                    } else if (state == state.FAILED) {
                        text_state.setText("连接失败");
                    }
               }

            }
        }
    };





    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    final ArrayList<String> fileNames = new ArrayList<>();
                    final ArrayList<String> paths = new ArrayList<>();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            String s=uri.getPath();
                            s=s.substring(5);
                            paths.add(s);
                            fileNames.add(uri.getLastPathSegment());
                        }
                        ArrayList<String> connectedIP = getConnectedIP();
                        for (String ip : connectedIP) {
                            if (ip.contains(".")) {
                                Message.obtain(handler, 0, "正在发送至"  + ip+":" +  PORT).sendToTarget();
                            }
                        }
                        Thread sendThread = new Thread(new Runnable(){
                            @Override
                            public void run() {
                                ArrayList<String> connectedIP = getConnectedIP();
                                for (String ip : connectedIP) {
                                    if (ip.contains(".")) {
                                        Log.w("AAA", "IP:" + ip);
                                        socketManager.SendFile(fileNames, paths,ip,9999 );
                                    }
                                }

                            }
                        });
                        sendThread.start();
                    }
                } else {
                    final ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);
                    final ArrayList<String> fileNames = new ArrayList<>();
                    if (paths != null) {
                        for (String path: paths) {
                            Uri uri = Uri.parse(path);
                            paths.add(uri.getPath());
                            fileNames.add(uri.getLastPathSegment());

                                ArrayList<String> connectedIP = getConnectedIP();
                                for (String ip : connectedIP) {
                                    if (ip.contains(".")) {
                                        Log.w("AAA", "IP:" + ip);
                                        socketManager.SendFile(fileNames, paths,ip,9999 );
                                    }
                                }
                        }
                        Message.obtain(handler, 0, "正在发送至" +  ":" +  9999).sendToTarget();
                        Thread sendThread = new Thread(new Runnable(){
                            @Override
                            public void run() {
                                    ArrayList<String> connectedIP = getConnectedIP();
                                    for (String ip : connectedIP) {
                                        if (ip.contains(".")) {
                                            Log.w("AAA", "IP:" + ip);
                                            socketManager.SendFile(fileNames, paths,ip,9999 );
                                        }
                                    }
                                }
                        });
                        sendThread.start();
                    }
                }
            }
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        System.exit(0);
    }

    private Handler handler=new Handler(){
        public void handleMessage(Message msg) {
            SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
            switch (msg.what) {
                case SEND_FILE:
                    txtEt.append("\n[" + format.format(new Date()) + "]" + msg.obj.toString());
                    break;
                case DEVICE_CONNECTING:
                    connectThread = new ConnectThread(listenerThread.getSocket(),handler);
                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    textview.setText("设备连接成功！可以进行文件传输及聊天");
                    break;
                case SEND_MSG_SUCCSEE:
                    txtEt.append("\n[" + format.format(new Date()) + "]"+"发送消息成功:" + msg.getData().getString("MSG"));
                    break;
                case SEND_MSG_ERROR:
                    txtEt.append("\n[" + format.format(new Date()) + "]"+"发送消息失败:" + msg.getData().getString("MSG"));
                    break;
                case GET_MSG:
                    txtEt.append("\n[" + format.format(new Date()) + "]"+"收到消息:" + msg.getData().getString("MSG"));
                    break;
            }
        }
    };

    public String GetIpAddress() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int i = wifiInfo.getIpAddress();
        return (i & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF)+ "." +
                ((i >> 24 ) & 0xFF );
    }
    private ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    View.OnClickListener hand=new View.OnClickListener(){
        public void onClick(View v){
            switch (v.getId()) {
                case R.id.btn_create_hostspot:
                    createWifiHotspot();
                    break;
                case R.id.btn_close_hostspot:
                    closeWifiHotspot();
                    break;
                case R.id.btn_close_wifi:
                    closeWifi();
                    break;
                case R.id.btn_send:
                    if (connectThread != null) {
                        chat_txt = messageEdit.getText().toString();
//                        Intent intent=new Intent();
//                        intent.setClass(FirstActivity.this, SecondActivity.class);//从一个activity跳转到另一个activity
//                        startActivity(intent);
                        connectThread.sendData(chat_txt);
                    }else{
                        Log.w("AAA","connectThread == null");
                    }
                    break;
                case R.id.btn_search:
                    search();
                    break;
                case R.id.btn_sendFile:


                    Intent i=new Intent(FirstActivity.this, FilePickerActivity.class);
                    i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                    i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                    i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                    i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                    startActivityForResult(i, FILE_CODE);

            }
        }
    };


    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    /************************************************************
     打开热点
     ************************************************************/

    private void createWifiHotspot(){
        if(wifiManager.isWifiEnabled()){
            //如果wifi处于打开状态，则关闭wifi
            wifiManager.setWifiEnabled(false);
        }
        WifiConfiguration config=new WifiConfiguration();
        config.SSID="test";
        config.preSharedKey="123456789";
        config.hiddenSSID=true;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);//打开系统认证
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        //通过反射调用设置热点
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManager, config, true);
            if (enable) {
                textview.setText("热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:123456789");
            } else {
                textview.setText("创建热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            textview.setText("创建热点失败");
        }
    }

    public void closeWifiHotspot() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration)method.invoke(wifiManager);
            Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(wifiManager, config, false);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        textview.setText("热点已关闭" );
    }


    //获得附近WiFi的信息
    public void onClickGetNearbyWifi(View view){
        List<ScanResult> list=wifiManager.getScanResults();
        StringBuilder buf=new StringBuilder();
        if (list!=null){
            for(ScanResult scanWifi:list){
                buf.append("SSID:"+scanWifi.SSID+",BSSID:"+scanWifi.BSSID+"/n");
            }
        }
        nearbyWifiTV.setText(buf.toString());
        /**
         BSSID 接入点的地址
         SSID 网络的名字，唯一区别WIFI网络的名字
         Capabilities 网络接入的性能
         Frequency 当前WIFI设备附近热点的频率(MHz)
         Level 所发现的WIFI网络信号强度*/
    }
    //获得以连接的WiFi的信息
    public void onClickGetConnectedWifi(View view){
        WifiInfo wifiInfo=wifiManager.getConnectionInfo();
        String text=null;
        if(wifiInfo!=null){
            text="SSID:"+wifiInfo.getBSSID()+",\nMacAddress:"+wifiInfo.getMacAddress();
        }
        connectWifiTV.setText(text);

        /**
         getBSSID() 获取BSSID属性
         getDetailedStateOf() 获取客户端的连通性
         getHiddenSSID() 获取SSID 是否被隐藏
         getIpAddress() 获取IP 地址
         getLinkSpeed() 获取连接的速度
         getMacAddress() 获取Mac 地址
         getRssi() 获取802.11n 网络的信号
         getSSID() 获取SSID
         getSupplicanState() 获取具体客户端状态的信息*/

    }
//改变wifi状态
    private void closeWifi(){
        if(wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(false);
        }
        textview.setText("wifi已经关闭");


    }
    private void refreshWifi(){
        if (wifiManager.isWifiEnabled()){
            Toast.makeText(FirstActivity.this, "wifi state:open", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(FirstActivity.this, "wifi state:close", Toast.LENGTH_SHORT).show();
        }
    }

    public WifiConfiguration createWifiInfo(String SSID, String password,
                                            int type) {
        Log.w("AAA", "SSID = " + SSID + "password " + password + "type ="
                + type);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement
                    .set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            return null;
        }
        return config;
    }


    private void search() {
        if (!wifiManager.isWifiEnabled()) {
            //开启wifi
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();
    }

}