package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsyncCompletion
{
    private static Logger log = Logger.getLogger("mwcmq2");

    private class ProcessThread extends Thread
    {
        ProcessThread()
        {
            setDaemon(true);
            setName("async completion process thread");
        }

        public void run()
        {
            List <Message> localList = new LinkedList<Message>();
            long lastPurge = System.currentTimeMillis();
            long timeNow = lastPurge;
            while(true)
            {
                boolean doPurge = false;
                synchronized(list)
                {
                    
                    while(list.isEmpty())
                    {
                        try
                        {
                            list.wait(60*1000);
                            // purge every 5 minutes.
                            timeNow = System.currentTimeMillis();
                            if(timeNow - lastPurge>1000*300)
                            {
                                doPurge = true;
                                lastPurge = timeNow;
                                break;
                            }
                        } catch(Exception ign) {}
                    }

                    localList.clear();
                    localList.addAll(list);
                    list.removeAll(list);
                }

                if(doPurge)
                {
                    synchronized(list)
                    {
                        log.info("Doing purge. Map.size="+map.size());

                        for(Iterator <String>itt=map.keySet().iterator();
                            itt.hasNext();)
                        {
                            try
                            {
                                String address = itt.next();
                                AsyncRequest request = map.get(address);
                                long lastSeenTime = mc.getLastSeenTime(address);
                                mc.getAndRemove(address); // clear messages
                                // if we haven't seen them for 5 minutes, remove listener.
                                if(timeNow-lastSeenTime>300*1000)
                                {
                                    itt.remove();
                                    log.info("Purged " + address + ". Inactive for " +
                                            (timeNow-lastSeenTime));
                                    try { request.ac.complete(); } catch(Exception ign) {}
                                }
                            }
                            catch(Exception err)
                            {
                                log.log(Level.SEVERE, "Purge generated exception", err);
                            }
                        }
                        log.info("Purge complete!");
                    }
                }

                for(Iterator <Message> itt=localList.iterator();
                        itt.hasNext();)
                {
                    try
                    {
                        Message message = itt.next();
                        log.info("Processing message: " + message);
                        AsyncRequest request = null;

                        synchronized(list)
                        {
                            request = map.get(message.address);
                        }

                        if(request != null)
                        {
                            synchronized(request)
                            {
                                try
                                {
                                    synchronized(list)
                                    {
                                        map.remove(message.address);
                                    }

                                    if(request.delCount>=0)
                                        mc.add(request.address, message.message, request.startTime);

                                    request.os.write(("message: " + message.message + "\n").getBytes());
                                    log.info("Returning: " + message.message);
                                    request.os.flush();

                                }
                                catch(Exception err)
                                {
                                    log.log(Level.SEVERE,
                                            "AsyncCompletion.ProcessThread" +
                                                    " generated exception",
                                                    err);
                                }
                                finally
                                {
                                    // we complete no matter what.
                                    try { request.ac.complete(); } catch(Exception err) {}
                                }
                            }
                        }
                        else
                        {
                            log.info("address " + message.address + " not connected now.");
                            // we use 0 start time because we don't know what the
                            // listener's start time will be, but will be great than 0.
                            mc.add(message.address, message.message, 0);
                        }
                    } catch(Exception err)
                    {
                        log.log(Level.SEVERE, "Message processing generated exception", err);
                    }
                }
            }
        }
    }
    
    public void closeConnection(String address)
    {
        AsyncRequest request = null;
        String message = "closenewlogin";

        synchronized(list)
        {
            request = map.get(address);
        }
        
        if(request == null) return; // if it's not connected nothing to do.
        
        synchronized(request)
        {
            try
            {
                synchronized(list)
                {
                    map.remove(address);
                }

                log.info("Returning: " + message);
                request.os.write(("message: " + message + "\n").getBytes());
                request.os.flush();

            }
            catch(Exception err)
            {
                log.log(Level.SEVERE,
                        "AsyncCompletion.ProcessThread" +
                                " generated exception",
                                err);
            }
            finally
            {
                // we complete no matter what.
                try { request.ac.complete(); } catch(Exception ign) {}
            }
        }
    }

    private MessageCache mc = null;
    private Map<String,AsyncRequest> map = null;
    private List<Message> list = null;
    
    public AsyncCompletion()
    {
        map = new HashMap<String,AsyncRequest>();
        list = new LinkedList<Message>();
        // we set the message cache to allow 10,000 users.
        // If more needed we'll increase for now this is fine.
        mc = new MessageCache(10000);
        new ProcessThread().start();
    }
    
    public void send(String address, String message)
    {
        Message m = new Message(address,
                                message,
                                System.currentTimeMillis());
        synchronized(list)
        {
            list.add(m);
            list.notify();
        }
    }
    
    public void updateLastSeenTime(String address)
    {
        mc.updateLastSeenTime(address);
    }
    
    public long getLastSeenTime(String address)
    {
        return mc.getLastSeenTime(address);
    }
    
    public void add(AsyncRequest req)
    {
        mc.setLastSeenTime(req.address, req.startTime);

        if(req.delCount>0)
            mc.removeTo(req.address, req.delCount);
        
        List <Message> messages = null;
        if(req.delCount < 0)
            messages = mc.getAndRemove(req.address);
        else
            messages = mc.getAll(req.address);

        if(messages != null && messages.size() != 0)
        {
            try
            {
                // if the user has messages, complete right now.
                req.os.write(("messagelist: " + messages.size() + "\n").getBytes());
                for(Iterator<Message>itt=messages.iterator(); itt.hasNext();)
                {
                    Message next = itt.next();
                    req.os.write(("message[" + next.message.length() + "]: " +
                            next.message + "\n").getBytes());
                }
                req.os.flush();
            }
            catch(Exception err)
            {
                log.log(Level.SEVERE,
                        "AsyncCompletion.add generated exception",
                        err);
            }
            req.ac.complete();
        }
        else
        {
            // otherwise add to listen thread for later processing.
            synchronized(list)
            {
                map.put(req.address, req);
            }
        }
    }

    public boolean isMostRecentAndSet(String address, long listenerTime, AsyncCompletion acomp)
    {
        return mc.isMostRecentAndSet(address, listenerTime, acomp);
    }


}
