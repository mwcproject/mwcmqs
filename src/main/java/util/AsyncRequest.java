package util;

import java.io.OutputStream;

import javax.servlet.AsyncContext;

public class AsyncRequest {
    public AsyncContext ac;
    public String address;
    public OutputStream os;
    public int delCount;

    public AsyncRequest(AsyncContext ac,
                        String address,
                        OutputStream os,
                        int delCount)
    {
        this.ac = ac;
        this.address = address;
        this.os = os;
        this.delCount = delCount;
    }
}