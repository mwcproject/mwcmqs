package mwcmqs;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

public class perfTest
{
    static AtomicInteger recvCount = new AtomicInteger(0);
    private class MWCMQSConnection {
        private String server = null;
        private String address = null;
        
        MWCMQSConnection(String server, String address)
        {
            if(server.endsWith("/"))
                this.server = server;
            else
                this.server = server + "/";
            
            this.address = address;
            
        }
        
        public String getAddress() { return address; }

        private void connect()
        {
            System.out.println("Connecting " + address);
            HttpsURLConnection con = null;
            BufferedReader buf = null;
            long startTime = System.currentTimeMillis();
            int delcount = 0;
            while(true)
            {
                try
                {

                    URL obj = new URL(server +
                            "listener?time_now="+
                            startTime+
                            "&delcount=" + delcount+
                            "&address=" + 
                            address);
                    String host = obj.getHost();
                    if(host != "mqs.mwc.mw")
                        obj = new URL(server +
                                "listener?time_now="+
                                startTime+
                                "&delcount=" + delcount+
                                "&address=" + 
                                address + "@" + host);
                    con = (HttpsURLConnection) obj.openConnection();
                    con.setConnectTimeout(3000);
                    con.setReadTimeout(120000);

                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", "mwcmqs 1.0");
                    con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                    buf = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    String line;

                    System.out.println("Blocking for reads");
                    delcount = 0;
                    while((line=buf.readLine()) != null)
                    {
                        System.out.println("listen["+address+"]=" + line);
                        if(line.indexOf("hey")>=0)
                            recvCount.incrementAndGet();
                        delcount++;
                    }
                }
                catch(SocketTimeoutException err)
                {
                    System.out.println("Expected timeout. Reconnecting.");
                }
                catch(Exception err)
                {
                    err.printStackTrace();
                }
                finally
                {
                    try { buf.close(); } catch(Exception ign) {}
                    try { con.disconnect(); } catch(Exception ign) {}
                }
            }
           
        }
    }
    
    private class ListenerThread extends Thread
    {
        private MWCMQSConnection conn = null;
        
        ListenerThread(MWCMQSConnection conn)
        {
            setDaemon(true);
            setName("Listener thread");
            this.conn = conn;
        }
        
        public void run()
        {
            conn.connect();
        }
        
        public String toString()
        {
            return "[listener for testaddress" + conn.getAddress() + "]";
        }
    }
    
    private void send(String server, String address, String message)
    {
        if(!server.endsWith("/"))
            server += "/";
        
        HttpsURLConnection con = null;
        DataOutputStream wr = null;
        BufferedReader buf = null;
        
        try
        {
            URL obj = new URL(server+"sender?address="+address);
            String host = obj.getHost();
            if(host != "mqs.mwc.mw")
                obj = new URL(server +"sender?address=" + address + "@" +
                              host);
            
            con = (HttpsURLConnection) obj.openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);

            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "mwcmqs 1.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            con.setDoOutput(true);
            wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(message);
            wr.flush();
            wr.close();

            buf = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));

            StringBuilder resp = new StringBuilder();
            String line;
            while((line=buf.readLine()) != null)
                resp.append(line);
            
            System.out.println("respsend="+resp.toString());
        } catch(Exception err)
        {
            err.printStackTrace();
        }
        finally
        {
            try { wr.close(); } catch(Exception ign) {}
            try { buf.close(); } catch(Exception ign) {}
            try { con.disconnect(); } catch(Exception ign) {}
            
            
        }
    }
    
    public perfTest(String [] args)
    {
        String randomPrefix = Long.toString(
                (long)Math.floor(Math.random() * Long.MAX_VALUE));
        String server = args[0];
        int count = Integer.parseInt(args[1]);
        MWCMQSConnection [] conns = new MWCMQSConnection[count];
        
        for(int i=0; i<count; i++)
        {
            conns[i] = new MWCMQSConnection(server, randomPrefix+"_testaddress"+i);
            new ListenerThread(conns[i]).start();
            System.out.println(conns[i]);
            try { Thread.sleep(1); } catch(Exception ign) {}
        }
        
        for(int i=0; i<count; i++)
        {
            send(server, randomPrefix+"_testaddress"+i, "hey"+i);
        }
        
        try { Thread.sleep(1000); } catch(Exception ign) {}
        System.out.println("Sent " + count +
                           " messages. Received " + recvCount.get() +
                           " messages.");
        
        try { Thread.sleep(10000); } catch(Exception ign) {}
        System.out.println("Sent " + count +
                           " messages. Received " + recvCount.get() +
                           " messages.");
        
    }

    public static void main(String [] args)
    {
        new perfTest(args);
    }
}
