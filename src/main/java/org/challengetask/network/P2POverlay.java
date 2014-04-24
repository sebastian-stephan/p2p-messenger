/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.challengetask.network;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.futures.FutureGet;
import net.tomp2p.futures.FuturePut;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.p2p.builder.ShutdownBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

/**
 *
 * @author Sebastian
 */
public class P2POverlay {

    private Peer peer;
    private static Random rnd = new Random();
    
    public PeerAddress getPeerAddress() {
        return peer.getPeerAddress();
    }
    
    public boolean put(String key, Object value) {
        Data data;
        try {
            data = new Data(value);
        } catch (IOException ex) {
            return false;
        }
        
        FuturePut futurePut = peer.put(Number160.createHash(key)).setData(data).start()
                   .awaitUninterruptibly();
        
        return futurePut.isSuccess();
    }
    
    public Object get(String key) {
        FutureGet futureGet = peer.get(Number160.createHash(key)).start().awaitUninterruptibly();
        
        if(futureGet.isSuccess()) {
            try {
                return futureGet.getData().object();
            } catch (ClassNotFoundException | IOException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean send(PeerAddress recipient, Object o) {
        FutureDirect futureDirect = peer.sendDirect(recipient)
                .setObject(o).start().awaitUninterruptibly();
        
        return futureDirect.isSuccess();
    }
    
    public Pair<Boolean,String> bootstrap() {
        int port = 4001;

        // Create TomP2P peer
        boolean peerCreated = false;
        do {
            try {
                // Create new peer
                peer = new PeerMaker(new Number160(rnd)).ports(port++).makeAndListen();
                peerCreated = true;
            } catch (IOException ex) {
                System.out.println("Port already in use. " + ex.getMessage());
            }
        } while (!peerCreated && port < 4010);

        if (!peerCreated) {
            return new Pair<>(false,"Could not find any unused port");
        }
        
        // Attach reply handler
        peer.setObjectDataReply(new ObjectReplyHandler());

        try {
            FutureBootstrap futureBootstrap = peer.bootstrap().setInetAddress(InetAddress.getByName("192.168.1.34")).setPorts(4001).start();
            futureBootstrap.awaitUninterruptibly();
            if (futureBootstrap.isSuccess())
                return new Pair<>(true, "Bootstrap successful");
            else
                return new Pair<>(false, "Could not bootstrap to well known peer");
        } catch (UnknownHostException ex) {
            return new Pair<>(false, "Unknown bootstrap host. (UnknownHostException)");
        }
        
    }

    public void shutdown() {
        System.out.println("Shutting down...");

        BaseFuture shutdownBuilder = peer.shutdown();

        System.out.println("Shutdown process started");

        shutdownBuilder.awaitUninterruptibly();

        if (shutdownBuilder.isCompleted()) {
            System.out.println("Shutdown is complete");
        }
        if (shutdownBuilder.isSuccess()) {
            System.out.println("Shutdown is Success");
        }
        if (shutdownBuilder.isFailed()) {
            System.out.println("Shutdown is Failed");
        }
        peer = null;
    }
}
