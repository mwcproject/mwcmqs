package util;

import java.io.PrintWriter;

import javax.servlet.AsyncContext;

public class AsyncRequest {
    public AsyncContext ac;
    public String address;
    public PrintWriter pw;

    public AsyncRequest(AsyncContext ac, String address, PrintWriter pw)
    {
        this.ac = ac;
        this.address = address;
        this.pw = pw;
    }
}