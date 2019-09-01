package util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MessageCache
{
    private class Entry
    {
        Entry()
        {
            messages = new LinkedList<Message>();
        }
        List <Message> messages;
        long lastSeenTime = -1;
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
                try { Thread.sleep(10*1000); } catch(Exception err)
                {
                    long timeNow = System.currentTimeMillis();
                    long totalMessageSize = 0;
                    long totalDeletedMessageSize = 0;
                    long totalMessageCount = 0;
                    long totalDeletedMessageCount = 0;
                    System.out.println("Executing cleaner");

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
                                    System.out.println(
                                            "Removing message: " + msg);
                                    entry.messages.remove(msg);
                                }
                            }

                        }
                    }
                    
                    System.out.println("Cleaner summary: ");
                    System.out.println(
                        "totalMessageCount="+totalMessageCount+
                        ",delMessageCount="+totalDeletedMessageCount+
                        ",totalMessageSize="+totalMessageSize+
                        ",delMessageSize="+totalDeletedMessageSize);
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
    
    public synchronized void setLastSeenTime(String address)
    {
        Entry entry = cache.get(address);
        if(entry == null)
        {
            entry = new Entry();
            cache.put(address, entry);
        }
        entry.lastSeenTime = System.currentTimeMillis();
    }
    
    public synchronized long getLastSeenTime(String address)
    {
        Entry entry = cache.get(address);
        if(entry == null)
            return -1;
        
        return entry.lastSeenTime;
    }

    public synchronized void add(String address, String message)
    {
        Entry entry = cache.get(address);
        if(entry == null)
        {
            entry = new Entry();
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
    
    public synchronized void removeTo(String address, int index)
    {
        Entry entry = cache.get(address);
        if(entry == null)
            return;
        
        for(int i=0; i<index; i++)
        {
            entry.messages.remove(0);
        }
    }
    
    // test program of simple map
    public static void main(String [] args)
    {
        MessageCache cm = new MessageCache(3);
        
        cm.add("1", "a");
        
        cm.add("2", "b");
        cm.add("2", "hihi");
        cm.add("3", "c");
        cm.add("4", "d");
        
        System.out.println("1="+cm.getAndRemove("1"));
        System.out.println("2="+cm.getAndRemove("2"));
        System.out.println("3="+cm.getAndRemove("3"));
        System.out.println("4="+cm.getAndRemove("4"));
        System.out.println("4="+cm.getAndRemove("4"));

    }
}