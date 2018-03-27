package com.example.user.myapplication;

import android.app.Application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SocketHandler extends Application {
    public static Socket socket = null;
    public static BufferedInputStream bis;
    public static BufferedOutputStream bos;
    public SocketHandler () {}
    public SocketHandler (String addr, int port) {
        try {
            InetAddress serverAddr = InetAddress.getByName(addr);
            socket = new Socket(serverAddr, port);
            bis = new BufferedInputStream(socket.getInputStream());
            bos = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
