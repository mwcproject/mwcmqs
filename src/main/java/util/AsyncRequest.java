package util;

import java.io.OutputStream;

import javax.servlet.AsyncContext;

public class AsyncRequest {
    public AsyncContext ac = null;
    public String address = null;
    public OutputStream os = null;
    public int delCount = -1;
    public long startTime = -1;
    public String delTo = null;

    public AsyncRequest(AsyncContext ac,
                        String address,
                        OutputStream os,
                        int delCount,
                        String delTo,
                        long startTime)
    {
        this.ac = ac;
        this.address = address;
        this.os = os;
        this.delCount = delCount;
        this.startTime = startTime;
        this.delTo = delTo;
    }
}