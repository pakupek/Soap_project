package server;
import javax.xml.ws.Endpoint;

public class ServerMain {
    public static void main(String[] args) {
        Endpoint.publish("http://192.168.0.193:8080/repair", new RepairServiceImpl());
        System.out.println("SOAP SERVER RUNNING: http://192.168.0.193:8080/repair?wsdl");
    }
}