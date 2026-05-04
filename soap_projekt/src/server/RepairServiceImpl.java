package server;
import client.InvoiceRequest;
import client.InvoiceResponse;
import client.RepairRequest;

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

    public InvoiceResponse createInvoice(InvoiceRequest req) {

        InvoiceResponse res = new InvoiceResponse();

        res.setInvoiceId(UUID.randomUUID().toString());

        // 🔥 dane od admina
        res.setActions(req.getActions());
        res.setLaborCost(req.getLaborCost());
        res.setPartsCost(req.getPartsCost());

        // 🔥 obliczenie
        res.setPrice(req.getLaborCost() + req.getPartsCost());

        res.setStatus("SENT");

        System.out.println("=== INVOICE SENT ===");
        System.out.println("Actions: " + res.getActions());
        System.out.println("Total: " + res.getPrice());

        // 🔥 przypięcie do zgłoszenia
        if (req.getRequestId() >= 0 && req.getRequestId() < requests.size()) {

            RepairRequest r = requests.get(req.getRequestId());

            r.setInvoice(res);
            r.setStatus("INVOICED");
        }

        return res;
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