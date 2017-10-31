package com.example.ply.activitytest.application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Application;

/**
 * Created by PLY on 2017/10/24.
 */

public class ApplicationUtil extends Application {
    private Socket socket;
    private DataOutputStream out = null;
    private DataInputStream in = null;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public void init() throws IOException, Exception{
        this.socket = new Socket("192.168.1.104",10202);
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    public Socket getSocket() {

        return socket;
    }

    public void setSocket(String ip,int port)throws IOException, Exception {
        this.socket=new Socket(ip,port);
    }

    public DataOutputStream getOut() {
        return out;
    }

    public void setOut(DataOutputStream out) {
        this.out = out;
    }

    public DataInputStream getIn() {
        return in;
    }

    public void setIn(DataInputStream in) {
        this.in = in;
    }

}
