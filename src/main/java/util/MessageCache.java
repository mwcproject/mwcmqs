package util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageCache
{
    private static Logger log = LoggerFactory.getLogger(MessageCache.class);

    private class Entry
    {
        Entry(long startTime, long lastSeenTime)
        {
            messages = new LinkedList<Message>();
            this.lastSeenTime = lastSeenTime;
            this.startTime = startTime;
        }

        Entry(long startTime)
        {
            messages = new LinkedList<Message>();
            lastSeenTime = System.currentTimeMillis();
            this.startTime = startTime;
        }
        List <Message> messages;
        long lastSeenTime = -1;
        long startTime = -1;
    }

    private class CacheMap <K,V> extends LinkedHashMap <K,V>
    {
        /**
         * 
         */
        private static final long serialVersionUID = -1243243708387485400L;

        private int maxCacheSize;
        
        public CacheMap(int max)
        {
            maxCacheSize = max;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry <K,V>eldest)
        {
            return size() > maxCacheSize;
        }
            
        @Override
        public boolean equals(Object obj)
        {
            return obj==this;
        }
            
        @Override
        public int hashCode()
        {
            return System.identityHashCode(this);
        }

    }
    
    private class Cleaner extends Thread
    {
        private Object lock = null;
        private Cleaner(Object lock)
        {
            setDaemon(true);
            setName("MessageCache cleaner thread");
            this.lock = lock;
        }
        public void run()
        {
            while(true)
            {
                // run every 5 minutes and cleanup
                // messages older than 5 minutes
                try { Thread.sleep(300*1000); } catch(Exception err) {}
                long timeNow = System.currentTimeMillis();
                long totalMessageSize = 0;
                long totalDeletedMessageSize = 0;
                long totalMessageCount = 0;
                long totalDeletedMessageCount = 0;
                log.info("Executing cleaner");

                try {
                synchronized(lock)
                {
                    for(Iterator <String> itt=cache.keySet().iterator();
                            itt.hasNext();)
                    {
                        String address = itt.next();
                        Entry entry = cache.get(address);

                        for(Iterator<Message> msgitt=
                                entry.messages.iterator();
                                msgitt.hasNext();)
                        {
                            Message msg = msgitt.next();
                            totalMessageCount++;
                            totalMessageSize += msg.message.length();
                            if(timeNow - msg.timeStamp > 300 * 1000)
                            {
                                totalDeletedMessageCount++;
                                totalDeletedMessageSize +=
                                        msg.message.length();
                                entry.messages.remove(msg);
                            }
                        }

                    }
                }

                log.info("Cleaner summary: ");
                log.info(
                        "totalMessageCount="+totalMessageCount+
                        ",delMessageCount="+totalDeletedMessageCount+
                        ",totalMessageSize="+totalMessageSize+
                        ",delMessageSize="+totalDeletedMessageSize);
                }
                catch(Exception err)
                {
                    log.error("Cleaner generated exception", err);
                    
                }
            }

        }
    }
    
    CacheMap <String, Entry> cache = null;
    
    public MessageCache(int capacity)
    {
        cache = new CacheMap<String, Entry>(capacity);
        new Cleaner(this).start();
    }
    
    public synchronized void setLastSeenTime(String address, long startTime)
    {
        Entry entry = cache.get(address);
        if(entry == null)
        {
            entry = new Entry(startTime);
            cache.put(address, entry);
        }
    }
    
    public synchronized void updateLastSeenTime(String address)
    {
        Entry entry = cache.get(address);
        
        if(entry != null)
        {
            entry.lastSeenTime = System.currentTimeMillis();
        }
        // don't worry about null case. It will be created with a current timestamp.
    }
    
    public synchronized long getLastSeenTime(String address)
    {
        Entry entry = cache.get(address);
        if(entry == null)
            return -1;
        
        return entry.lastSeenTime;
    }

    public synchronized void add(String address, String message, long startTime, boolean connected)
    {
        Entry entry = cache.get(address);
        if(entry == null)
        {
            if(connected)
                entry = new Entry(startTime, System.currentTimeMillis());
            else
                entry = new Entry(startTime, -1);
            cache.put(address, entry);
        }

        entry.messages.add(
                new Message(address,
                            message,
                            System.currentTimeMillis()));
    }

    public synchronized List<Message> getAndRemove(String address)
    {
        Entry entry = cache.get(address);
        if(entry == null)
            return null;
        List <Message> ret = new LinkedList<Message>(entry.messages);
        entry.messages.removeAll(entry.messages);
        return ret;
    }
    
    public synchronized List<Message> getAll(String address)
    {
        Entry entry = cache.get(address);
        if(entry == null)
            return null;
        List <Message> ret = new LinkedList<Message>(entry.messages);
        return ret;
    }
    
    public synchronized void removeTo(String address, String delTo)
    {
        // if this is set to nil, it means don't delete anything.
        if("nil".equals(delTo))
            return;

        Entry entry = cache.get(address);
        if(entry == null || entry.messages == null)
            return;
        
        int size = entry.messages.size();
        int delCount = 0;
        
        for(int i=0; i<size; i++)
        {
            Message msg = entry.messages.get(i);
            if(msg.message.startsWith(delTo+";"))
            {
                delCount = i+1;
                break;
            }
        }
        
        for(int i=0; i<delCount; i++)
        {
            entry.messages.remove(0);
        }
    }
    
    public synchronized void removeTo(String address, int index)
    {
        Entry entry = cache.get(address);
        if(entry == null || entry.messages == null)
            return;
        
        for(int i=0; i<index; i++)
        {
            if(entry.messages.size()>0)
                entry.messages.remove(0);
        }
    }
    
    public synchronized boolean isMostRecentAndSet(String address,
                                                   long listenerTime,
                                                   AsyncCompletion acomp)
    {
        Entry entry = cache.get(address);
        if(entry == null)
            return true;

        if(listenerTime >= entry.startTime)
        {
            // also wakeup the other connection to disconnect immediately
            // but only iff it's not us. If listenerTime = entry.startTime
            // the request came from us.
            if(listenerTime > entry.startTime)
                acomp.closeConnection(address);
            
            // now we need to set it to stay valid.
            entry.startTime = listenerTime;
           
            
            return true;
        }
        
        return false;
    }
    
    // test program of simple map
    public static void main(String [] args)
    {
        MessageCache cm = new MessageCache(3);
        
        cm.add("1", "a", 0L, true);
        
        cm.add("2", "b", 0L, true);
        cm.add("2", "hihi", 0L, true);
        cm.add("3", "c", 0L, true);
        cm.add("4", "d", 0L, true);
        
        System.out.println("1="+cm.getAndRemove("1"));
        System.out.println("2="+cm.getAndRemove("2"));
        System.out.println("3="+cm.getAndRemove("3"));
        System.out.println("4="+cm.getAndRemove("4"));
        System.out.println("4="+cm.getAndRemove("4"));

    }
}