package model;
public class Process {

    private String id;
    private String address;
    private String port;

    public Process(String id, String address, String port){
        this.id = id;
        this.address = address;
        this.port = port;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
