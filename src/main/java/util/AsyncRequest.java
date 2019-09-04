package util;

import java.io.OutputStream;

import javax.servlet.AsyncContext;

public class AsyncRequest {
    public AsyncContext ac;
    public String address;
    public OutputStream os;
    public int delCount;
    public long startTime;

    public AsyncRequest(AsyncContext ac,
                        String address,
                        OutputStream os,
                        int delCount,
                        long startTime)
    {
        this.ac = ac;
        this.address = address;
        this.os = os;
        this.delCount = delCount;
        this.startTime = startTime;
    }
}