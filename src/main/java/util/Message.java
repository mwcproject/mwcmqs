package util;

public class Message {
    public String address;
    public String message;

    public String toString()
    {
        return "[address="+address+",message="+message+"]";
    }
    
    public Message(String address, String message)
    {
        this.address = address;
        this.message = message;
    }
}