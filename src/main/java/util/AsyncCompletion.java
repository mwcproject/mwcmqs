package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AsyncCompletion
{
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
            while(true)
            {
                synchronized(list)
                {
                    while(list.isEmpty())
                    {
                        try
                        {
                            list.wait();
                        } catch(Exception ign) {}
                    }
                    localList.clear();
                    localList.addAll(list);
                    list.removeAll(list);
                }

                System.out.println("LocalList: " + localList);
                for(Iterator <Message> itt=localList.iterator();
                    itt.hasNext();)
                {
                    Message message = itt.next();
                    System.out.println("Processing message: " + message);
                    AsyncRequest request = null;

                    synchronized(list)
                    {
                        request = map.get(message.address);
                    }

                    if(request != null)
                    {
                        try
                        {
                            request.os.write(("message: " + message.message + "\n").getBytes());
                            System.out.println("Returning: " + message.message);
                            request.os.flush();
                            synchronized(list)
                            {
                                map.remove(message.address);
                            }
                        }
                        catch(Exception err)
                        {
                            err.printStackTrace();
                        }
                        finally
                        {
                            // we complete no matter what.
                            request.ac.complete();
                        }
                    }
                    else
                    {
                        System.out.println("address " + message.address + " not connected now.");
                        mc.add(message.address, message.message);
                    }
                }
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
        Message m = new Message(address, message);
        synchronized(list)
        {
            list.add(m);
            list.notify();
        }
    }
    
    public long getLastSeenTime(String address)
    {
        return mc.getLastSeenTime(address);
    }
    
    public void add(AsyncRequest req)
    {
        mc.setLastSeenTime(req.address);

        List <Message> messages = mc.getAndRemove(req.address);
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
                err.printStackTrace();
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


}
