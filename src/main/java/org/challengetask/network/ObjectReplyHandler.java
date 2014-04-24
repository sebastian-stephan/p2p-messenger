/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.challengetask.network;

import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

/**
 *
 * @author Sebastian
 */
public class ObjectReplyHandler implements ObjectDataReply {

    
    
    @Override
    public Object reply(PeerAddress pa, Object o) throws Exception {
        System.out.println(pa.getInetAddress() + " sent something");
        return null;
    }
    
}
