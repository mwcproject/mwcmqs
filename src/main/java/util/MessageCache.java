package util;

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
    
    CacheMap <String, Entry> cache = null;
    
    public MessageCache(int capacity)
    {
        cache = new CacheMap<String, Entry>(capacity);
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
        entry.messages.add(new Message(address, message));
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