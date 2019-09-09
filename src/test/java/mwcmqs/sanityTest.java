package mwcmqs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@RunWith(JUnitPlatform.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class sanityTest
{
    @BeforeAll
    public static void init()
    {
        System.out.println("Intializing jetty instance");
        try {
            Runtime.getRuntime().exec("./jetty/start.sh").waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @AfterAll
    public static void teardown()
    {
        System.out.println("Stopping jetty instance");
        try {
            Runtime.getRuntime().exec(new String[] {
                                "./jetty/bin/jetty.sh",
                                "stop"})
            .waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void send(String server, String address, String message)
    {
        if(!server.endsWith("/"))
            server += "/";
        
        HttpURLConnection con = null;
        DataOutputStream wr = null;
        BufferedReader buf = null;
        
        try
        {
            URL obj = new URL(server+"sender?address="+address);
            
            con = (HttpURLConnection) obj.openConnection();
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

            assertEquals(resp.toString().startsWith("lastSeen: "), true);
        } catch(Exception err)
        {
            err.printStackTrace();
            // should not get here during tests. so fail.
            assertEquals(true, false);
        }
        finally
        {
            try { wr.close(); } catch(Exception ign) {}
            try { buf.close(); } catch(Exception ign) {}
            try { con.disconnect(); } catch(Exception ign) {}
        }
    }
    
    private void listen(String server,
                        String address,
                        boolean expectTimeout,
                        List<String> expectedMessages)
    {
        if(!server.endsWith("/"))
            server += "/";
        
        HttpURLConnection con = null;
        BufferedReader buf = null;
        long startTime = System.currentTimeMillis()-1000;
        int delcount = 0;
        
        
        try
        {

            URL obj = new URL(server +
                    "listener?time_now="+
                    startTime+
                    "&delcount=" + delcount+
                    "&address=" +
                    address);

            con = (HttpURLConnection) obj.openConnection();
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);

            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "mwcmqs 1.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            buf = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));

            String line;

            delcount = 0;
            int count = 0;
            while((line=buf.readLine()) != null)
            {
                if(count == 0)
                    assertEquals(line,
                                 "messagelist: " + expectedMessages.size());
                else
                {
                    assertEquals(line.substring(line.indexOf(';')+1),
                                 expectedMessages.get(count-1));
                }
                count++;
            }
        }
        catch(SocketTimeoutException err)
        {
            assertEquals(expectTimeout, true);
            return;
        }
        catch(Exception err)
        {
            err.printStackTrace();
            // should not get here during tests. so fail.
            assertEquals(true, false);
        }
        finally
        {
            try { buf.close(); } catch(Exception ign) {}
            try { con.disconnect(); } catch(Exception ign) {}
        }
        
        assertEquals(expectTimeout, false);

    }

    @Test
    public void testBasicSend()
    {
        send("http://localhost:8090", "abc", "hi");
    }
    
    @Test
    public void testBasicListen()
    {
        listen("http://localhost:8090", "xyz", true, null);
    }
    
    @Test
    public void testMessage()
    {
        send("http://localhost:8090", "a1111", "hi2");
        List<String> expected = new ArrayList<String>();
        expected.add("hi2");
        listen("http://localhost:8090", "a1111", false, expected);
    }
    
    @Test
    public void testMultiMessage()
    {
        send("http://localhost:8090", "a2222", "msg1");
        send("http://localhost:8090", "a2222", "msg2");
        send("http://localhost:8090", "a2222", "msg3");
        List<String> expected = new ArrayList<String>();
        expected.add("msg1");
        expected.add("msg2");
        expected.add("msg3");
        
        listen("http://localhost:8090", "a2222", false, expected);
    }
}
