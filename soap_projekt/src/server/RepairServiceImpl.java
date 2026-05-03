package server;
import java.util.UUID;
import javax.jws.WebService;
import java.util.ArrayList;
import java.util.List;

@WebService(endpointInterface = "client.RepairService")
public class RepairServiceImpl implements client.RepairService {

    private static List<client.RepairRequest> requests = new ArrayList<>();

    public client.InvoiceResponse sendRepairRequest(client.RepairRequest request) {

        request.setStatus("NEW");

        requests.add(request);

        System.out.println("=== NEW REQUEST ===");
        System.out.println("Client: " + request.getClientName());
        System.out.println("Device: " + request.getDevice());
        System.out.println("Images: " + request.getImagesBase64().size());

        client.InvoiceResponse inv = new client.InvoiceResponse();
        inv.setInvoiceId(UUID.randomUUID().toString());
        inv.setPrice(120 + request.getImagesBase64().size() * 25);
        inv.setStatus("CREATED");

        System.out.println("Invoice generated: " + inv.getInvoiceId());

        return inv;
    }

    public List<client.RepairRequest> getAllRequests() {
        return requests;
    }

    public void updateStatus(int index, String status) {
        if (index >= 0 && index < requests.size()) {
            requests.get(index).setStatus(status);
        }
    }

}