/*
 * The MIT License
 *
 * Copyright 2011 Alessio Di Pietro.
 * Copyright 2012 Marco Carbone
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.clever.HostManager.Dispatcher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.CleverMessage;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.Common.XMPPCommunicator.NotificationOperation;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

/**
 *
 * @author alessiodipietro
 */
class NotificationThread extends Thread implements PacketListener 
{

    private Queue<CleverMessage> queue = new LinkedList();
    private ConnectionXMPP connectionXMPP;
    boolean queueNotEmpty = false;
    boolean CMisPresent = false;
    private int notificationThreshold;
    private Logger logger=null;
//    private boolean flagsend=false;
   // private DispatcherAgent dis;

    public NotificationThread(ConnectionXMPP connectionXMPP, int notificationThreshold) {
//        try {
            logger=Logger.getLogger("NotificationThread");
            this.connectionXMPP = connectionXMPP;
            this.connectionXMPP.addPresenceListener(ConnectionXMPP.ROOM.CLEVER_MAIN, this);
            this.notificationThreshold = notificationThreshold;
            //this.dis=disp;
            this.setName("NotificationThread");
            
//            this.sleep(60000);
//        } catch (InterruptedException ex) {
//            java.util.logging.Logger.getLogger(NotificationThread.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        
    }

    public synchronized void sendCleverMsg(CleverMessage msg) {
        if (queue.size() > this.notificationThreshold) {
            queue.poll();
        }
        queue.add(msg);
        queueNotEmpty = true;
        this.notifyAll();
    }

    @Override
    public synchronized void run() {
        String target = null;
        while (true) {
            //no messages
            if (queue.isEmpty()) {
                try {
                    //this.sleep(20000);
                    queueNotEmpty = false;
                    while (!queueNotEmpty) {
//                        logger.info("?=) \ntarget: "+this.connectionXMPP.getActiveCC(ConnectionXMPP.ROOM.CLEVER_MAIN)+"\nnumcc:"+this.connectionXMPP.getNum_CCsInRoom(ConnectionXMPP.ROOM.CLEVER_MAIN)+"\n"+this.connectionXMPP.SearchCM_InRoom("cmcsk-laptop", ConnectionXMPP.ROOM.CLEVER_MAIN));
//                        
//                        this.sleep(10000);
                        this.wait();
                    }
                } catch (InterruptedException ex) {
                    logger.error("InterruptedException: "+ex);
                }
            }
            target = connectionXMPP.getActiveCC(ConnectionXMPP.ROOM.CLEVER_MAIN);
            logger.debug("?=) target="+target);
            
            
            //no active cc
            if (target == null) {
                try {
                    CMisPresent = false;
                    while (!CMisPresent) {
                        wait();
                    }
                  target = connectionXMPP.getActiveCC(ConnectionXMPP.ROOM.CLEVER_MAIN);
                } catch (InterruptedException ex) {
                    logger.error("InterruptedException: "+ex);
                }
            }
//            if(target == null || target.equals("")){
//               // try {
//                    
//                    target="cmcsk-laptop"; //DA ELIMINARE 
//               //} catch (InterruptedException ex) {                    
//                //   java.util.logging.Logger.getLogger(NotificationThread.class.getName()).log(Level.SEVERE, null, ex);
//               //}
//               
//            }
//            
//            if(!flagsend){
//                try {
//                    logger.debug("?=) sleeping ");
//                    this.sleep(60000);
//                    flagsend=true;
//                } catch (InterruptedException ex) {
//                    java.util.logging.Logger.getLogger(NotificationThread.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            
//            }
//            logger.debug("?=) flagsend= "+flagsend); 
            CleverMessage msg = queue.poll();
            msg.setDst(target);
            
            logger.debug("?=) send message... target="+target+" msg= \n"+msg.toXML());
            connectionXMPP.sendMessage(target, msg);     
            
            
            

        }
    }

    @Override
    public synchronized void processPacket(Packet packet) {
        if (connectionXMPP.getActiveCC(ConnectionXMPP.ROOM.CLEVER_MAIN) != null) {
            CMisPresent = true;
            this.notifyAll();
        }
    }
    
   
}

public class DispatcherAgent extends Agent 
{    
    private Class cl = null;
    private ConnectionXMPP connectionXMPP = null;
    private NotificationThread notificationThread;
    private int notificationsThreshold;    
   

    public DispatcherAgent(ConnectionXMPP connectionXMPP, int notificationsThreshold)
    {   super();
        logger = Logger.getLogger("DispatcherAgentHm");
        this.connectionXMPP = connectionXMPP;
        this.notificationsThreshold = notificationsThreshold;
        
    }
    public DispatcherAgent(){
        super();
        logger=Logger.getLogger("DispatcherAgentHm");
    }
    
     @Override
public void initialization() throws CleverException
{
    super.setAgentName("DispatcherAgentHm");    
    super.start();
    
    
    logger.info("?=)DispatcherAgentHm ");//\ntarget: "+this.connectionXMPP.getActiveCC(ConnectionXMPP.ROOM.CLEVER_MAIN)+"\nnumcc:"+this.connectionXMPP.getNum_CCsInRoom(ConnectionXMPP.ROOM.CLEVER_MAIN)+"\n"+this.connectionXMPP.SearchCM_InRoom("cmcsk-laptop", ConnectionXMPP.ROOM.CLEVER_MAIN));
    notificationThread = new NotificationThread(connectionXMPP, notificationsThreshold);
    notificationThread.start();   
    
    
    String hostid=this.connectionXMPP.getHostName();
     Notification notification=new Notification();
     notification.setId("PRESENCE/HM");
     logger.debug("?=)** hostId= "+hostid);
     notification.setHostId(hostid);
     this.sendNotification(notification);      
           
    
    
    
}

    @Override
    public void sendNotification(Notification notification) {

        //TODO: send notification (via CleverMsg) to active CC
        logger.debug("preparing notify clever message");
        logger.debug("?=) notification body: "+notification.getAgentId());
        CleverMessage cleverMsg = new CleverMessage();
        List attachments=new ArrayList();
        attachments.add(notification.getBody());
        cleverMsg.fillMessageFields(CleverMessage.MessageType.NOTIFY, this.connectionXMPP.getUsername(), attachments, new NotificationOperation(connectionXMPP.getUsername(), notification.getAgentId(), notification.getId()));
        
        logger.debug("?=) clevermessage body: \n"+cleverMsg.getBody());
        
       
        
        
        //cleverMsg.setBody(MessageFormatter.messageFromObject(notification));
        notificationThread.sendCleverMsg(cleverMsg);
        logger.debug("notification sent");


    }

    @Override
    public Class getPluginClass() {
        return this.getClass();
    }

    @Override
    public Object getPlugin() {
        return this;
    }

    /**
     * @param notificationThreshold the notificationThreshold to set
     */
    public void setNotificationThreshold(int notificationsThreshold) {
        this.notificationsThreshold = notificationsThreshold;
    }
    
    @Override
   public void shutDown()
    {
        
    }
    
    public String getHcTarget(){
        try {
            //invoco il metodo del hostcoordinator per avere il target
                List params = new ArrayList();
                String target =(String) this.invoke("HostCoordinator", "getTarget", true, params);
                
                logger.info("?=) target da HC: "+target);
                return target;
        } catch (CleverException ex) {
            logger.info("Exception"+ex);
            return null;
        }
}
    
    
    
}
