package com.example.ply.activitytest.thread;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.ply.activitytest.FirstActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;


/**
 * Created by PLY on 2017/10/21.
 */

public class ConnectThread extends Thread {
    private final Socket socket;
    private Handler handler;
    private InputStream inputStream;
    private OutputStream outputStream;

    public ConnectThread(Socket socket,Handler handler){
        setName("ConnectThread");
        Log.w("1","ConnectThread");
        this.socket=socket;
        this.handler=handler;
    }

    public void run(){
        if(socket==null){
            return;
        }
        handler.sendEmptyMessage(FirstActivity.DEVICE_CONNECTED);
        try {
            //获取数据流
            inputStream =socket.getInputStream();
            outputStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;
            while (true){
                //读取数据
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    final byte[] data = new byte[bytes];
                    System.arraycopy(buffer, 0, data, 0, bytes);

                    Message message = Message.obtain();
                    message.what = FirstActivity.GET_MSG;
                    Bundle bundle = new Bundle();
                    bundle.putString("MSG",new String(data));
                    message.setData(bundle);
                    handler.sendMessage(message);

                    Log.w("AAA","读取到数据:"+new String(data));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void sendData(String msg){
        Log.w("AAA","发送数据:"+(outputStream==null));
        if(outputStream!=null){
            try {
                outputStream.write(msg.getBytes());
                Log.w("AAA","发送消息："+msg);
                Message message = Message.obtain();
                message.what = FirstActivity.SEND_MSG_SUCCSEE;
                Bundle bundle = new Bundle();
                bundle.putString("MSG",new String(msg));
                message.setData(bundle);
                handler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = FirstActivity.SEND_MSG_ERROR;
                Bundle bundle = new Bundle();
                bundle.putString("MSG",new String(msg));
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }
public void SendFile(ArrayList<String>fileName,ArrayList<String>path){
    try {
        for(int i=0;i<fileName.size();i++){

            OutputStreamWriter outputStreamWriter=new OutputStreamWriter(outputStream);
            BufferedWriter bwName=new BufferedWriter(outputStreamWriter);
            bwName.write(fileName.get(i));
            bwName.close();
            outputStreamWriter.close();
            SendMessage(0, "正在发送" + fileName.get(i));

            FileInputStream fileInput = new FileInputStream(path.get(i));
            int size = -1;
            byte[] buffer = new byte[1024];
            while((size = fileInput.read(buffer, 0, 1024)) != -1){
                outputStream.write(buffer, 0, size);
            }
            outputStream.close();
            fileInput.close();
            socket.close();
            SendMessage(0, fileName.get(i) + "  发送完成");
        }
        SendMessage(0, "所有文件发送完成");
    }catch (Exception e) {
        SendMessage(0, "发送错误:\n" + e.getMessage());
    }
}

    void SendMessage(int what, Object obj){
        if (handler != null){
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }

    void ReceiveFile(){
        try{

            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(streamReader);
            String fileName = br.readLine();
            br.close();
            streamReader.close();
            SendMessage(0, "正在接收:" + fileName);

            InputStream dataStream = socket.getInputStream();
            String savePath = Environment.getExternalStorageDirectory().getPath() + "/" + fileName;
            FileOutputStream file = new FileOutputStream(savePath, false);
            byte[] buffer = new byte[1024];
            int size = -1;
            while ((size = dataStream.read(buffer)) != -1){
                file.write(buffer, 0 ,size);
            }
            file.close();
            dataStream.close();
            socket.close();
            SendMessage(0, fileName + "接收完成");
        }catch(Exception e){
            SendMessage(0, "接收错误:\n" + e.getMessage());
        }
    }



}
