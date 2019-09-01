package util;

public class Message {
    public String address;
    public String message;
    public long timeStamp;

    public String toString()
    {
        return "[address="+address+
                ",message="+message+
                ",timestamp=" + timeStamp +"]";
    }
    
    public Message(String address, String message, long timeStamp)
    {
        this.address = address;
        this.message = message;
        this.timeStamp = timeStamp;
    }
}