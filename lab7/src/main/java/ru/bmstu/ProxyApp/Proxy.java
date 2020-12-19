package ru.bmstu.ProxyApp; 

import org.zeromq.*;

import ru.bmstu.ProxyApp.Partitions;
import ru.bmstu.ProxyApp.Client;
import ru.bmstu.ProxyApp.Cache;

import java.util.HashMap;

public class Proxy {

    private ZContext conn;
    private ZMQ.Socket frontend,backend;
    private HashMap<ZFrame,Partitions> processor; //The ZFrame class provides methods to send and receive single message
                                                  //ZMsg - contains several ZFrames  
    private long time;
    private static final long TIME_EPSILON = 5000;
    private static final String SPACE = " ";
    private static final int FRONTEND_SLOT = 0;
    private static final int BACKEND_SLOT = 1;

    public Proxy(ZContext conn) {
        this.conn = conn;

        this.frontend = this.conn.createSocket(SocketType.ROUTER);
        this.backend = this.conn.createSocket(SocketType.ROUTER);

        this.processor = new HashMap<>();

        this.frontend.setHWM(0);
        this.backend.setHWM(0);

        this.frontend.bind(Client.ADDRESS);
        this.backend.bind(Cache.ADDRESS);
        handle();
    }

    private void handle() {
        ZMQ.Poller pollers = conn.createPoller(2);

        pollers.register(frontend, ZMQ.Poller.POLLIN);
        pollers.register(backend, ZMQ.Poller.POLLIN);

        time = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            pollers.poll(1);

            if ((!processor.isEmpty()) && (System.currentTimeMillis() - time > TIME_EPSILON)) {
                removeDead();
                time = System.currentTimeMillis();
            }

            if (pollers.pollin(FRONTEND_SLOT)) {
                ZMsg msg = ZMsg.recvMsg(frontend);
                System.out.println("Received msg from frontend");

                if (msg != null) {
                    handleClientMsg(msg);
                } else {
                    break;
                }
            }

            if (pollers.pollin(BACKEND_SLOT)) {
                ZMsg msg = ZMsg.recvMsg(backend);

                if (msg != null) {
                    handleCacheMsg(msg);
                } else {
                    break;
                }
            }
        }
    }

    private void removeDead() {
        processor.entrySet().removeIf(com -> ((time-com.getValue().getTime()) > TIME_EPSILON * 1.5));
    }

    private void handleClientMsg(ZMsg msg) {
        String[] data = msg.getLast().toString().split(SPACE);

        //Client request format : PUT/GET <something>

        switch(data[0]) {
            case "PUT": {
                receivePutClientSignal(data,msg);
            }
            case "GET": {
                receiveGetClientSignal(data,msg);
            }
            default: {
                errorFeedback(frontend,"error: ",msg);
            }
        }
    }

    private void receiveGetClientSignal(String[] data, ZMsg msg) {
        for (HashMap.Entry<ZFrame,Partitions> c : processor.entrySet())
            if (c.getValue().belongTo(data[1])) {
                //req from client belong to 1 of partitions if cache
                ZFrame cache = c.getKey().duplicate();
                msg.addFirst(cache);
                msg.send(backend);
                System.out.println("received get request from client");
            }
    }

    private void receivePutClientSignal(String[] data, ZMsg msg) {
        for (HashMap.Entry<ZFrame,Partitions> c : processor.entrySet())
            if (c.getValue().belongTo(data[1])) {
                //req from client belong to 1 of partitions if cache
                ZFrame cache = c.getKey().duplicate();
                msg.addFirst(cache);
                msg.send(backend);
                System.out.println("received put request from client");
            }
    }

    private void handleCacheMsg(ZMsg msg) {
        String[] data = msg.getLast().toString().split(SPACE);

        if (msg.getLast().toString().contains("UPDATE")) {
            if (!processor.containsKey(msg.getFirst())) {
                Partitions pat = new Partitions(data[1], data[2], System.currentTimeMillis());
                processor.put(msg.getFirst().duplicate(),pat);
            } else {
                processor.get(msg.getFirst().duplicate()).setTime(System.currentTimeMillis());
            }
        } else {
            msg.pop();
            msg.send(frontend);
        }
    }

    private void errorFeedback(ZMQ.Socket socket, String error, ZMsg msg) {
        ZMsg e = new ZMsg();
        e.add(msg.getFirst() + " " + error);
        e.send(socket);
    }
}