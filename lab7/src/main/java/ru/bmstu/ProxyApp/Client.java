package ru.bmstu.ProxyApp.Client;

import org.zeromq.*;

import java.util.Scanner;

public class Client {
    public String ADDRESS = "tcp://localhost:2000";

    private static ZContext context; 
    private static ZMQ.Socket socket;

    public static void main(String args[]) {
        System.out.println("Client is trying connect to server");
        try {

            context = new ZContext();
            socket = context.createSocket(SocketType.REQ);
            socket.connect(ADDRESS);
            Scanner in = new Scanner(System.in);
            while (true) {
                String clientMsg = in.nextLine();

                ZMsg zmsgReq = new ZMsg();
                ZMsg zmsgRes = new ZMsg();

                if (clientMsg.contains("PUT") || clientMsg.contains("GET")) {
                    
                    zmsgReq.add(clientMsg);
                    zmsgReq.send(socket);
                    zmsgRes = ZMsg.recvMsg(socket);

                    if (zmsgReq) {
                        System.out.println("RESPONSE FROM PROXY: " + zmsgRes.popString());
                    } else {
                        System.out.println("NO RESPONSE FROM PROXY");
                    }

                } else { 
                    System.out.println("Client message only support PUT/GET method");
                }

            }


        } catch (ZMQException ex) {
            ex.printStackTrace();
        }
    }
}