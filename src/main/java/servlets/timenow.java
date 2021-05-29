package servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class timenow extends HttpServlet
{
    /**
     * 
     */
    private static final long serialVersionUID = 1026537920201200646L;

    public void doGet(HttpServletRequest req, HttpServletResponse res)
    {
        // Add CORS header
        res.addHeader("Access-Control-Allow-Origin", "*");
        
        try {
            String timeNow = Long.toString(System.currentTimeMillis());
            res.getOutputStream().write(timeNow.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
