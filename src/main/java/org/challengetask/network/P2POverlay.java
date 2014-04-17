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
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.peers.Number160;

/**
 *
 * @author Sebastian
 */
public class P2POverlay {

    private Peer peer;
    private static Random rnd = new Random();

    public String bootstrap() {
        int port = 4001;
        boolean peerCreated = false;
        do {
            try {
                // Create new peer
                peer = new PeerMaker(new Number160(rnd)).setPorts(port++).makeAndListen();
                peerCreated = true;
            } catch (Exception ex) {
                System.out.println("Port already in use " + ex.getMessage());
            }
        } while (!peerCreated && port < 4010);

        if (!peerCreated) {
            return "Could not find any unused port :(";
        }

        try {
                FutureBootstrap futureBootstrap = peer.bootstrap().setInetAddress(InetAddress.getByName("127.0.0.1")).setPorts(4001).start();
                futureBootstrap.awaitUninterruptibly();
                return "Bootstrapped: " + futureBootstrap.isSuccess();
        } catch (UnknownHostException ex) {
            return "Unknown host";
        }

    }

    public void shutdown() {
        System.out.println("Shutting down...");
        peer.shutdown();
        peer = null;
    }
}
