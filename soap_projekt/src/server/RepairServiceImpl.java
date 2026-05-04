package server;

import client.InvoiceRequest;
import client.InvoiceResponse;
import client.RepairRequest;
import client.RepairService;

import javax.jws.WebService;
import java.util.List;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicInteger;

@WebService(endpointInterface = "client.RepairService")
public class RepairServiceImpl implements RepairService {

    private static final AtomicInteger nextId = new AtomicInteger(0); // tymczasowo

    private List<RepairRequest> requests = initRequests();

    private static List<RepairRequest> initRequests() {
        List<RepairRequest> loaded = DataStore.load();
        int maxId = loaded.stream()
                .mapToInt(RepairRequest::getId)
                .max()
                .orElse(-1);
        nextId.set(maxId + 1);
        return loaded;
    }
    // =========================
    // NEW REQUEST
    // =========================
    @Override
    public InvoiceResponse sendRepairRequest(RepairRequest request) {

        request.setId(nextId.getAndIncrement());
        request.setStatus("NEW");
        requests.add(request);

        DataStore.save(requests);

        System.out.println("=== NEW REQUEST ===");
        System.out.println("Client: " + request.getClientName());
        System.out.println("Device: " + request.getDevice());
        System.out.println("Images: " + request.getImagesBase64().size());

        InvoiceResponse inv = new InvoiceResponse();
        inv.setInvoiceId(UUID.randomUUID().toString());
        inv.setPrice(120 + request.getImagesBase64().size() * 25);
        inv.setStatus("CREATED");

        System.out.println("Invoice generated: " + inv.getInvoiceId());

        return inv;
    }

    // =========================
    // CREATE INVOICE
    // =========================
    @Override
    public InvoiceResponse createInvoice(InvoiceRequest req) {

        InvoiceResponse res = new InvoiceResponse();

        res.setInvoiceId(UUID.randomUUID().toString());
        res.setActions(req.getActions());
        res.setLaborCost(req.getLaborCost());
        res.setPartsCost(req.getPartsCost());

        res.setPrice(req.getLaborCost() + req.getPartsCost());
        res.setStatus("SENT");

        System.out.println("=== INVOICE SENT ===");
        System.out.println("Actions: " + res.getActions());
        System.out.println("Total: " + res.getPrice());

        requests.stream()
                .filter(r -> r.getId() == req.getRequestId())
                .findFirst()
                .ifPresent(r -> {
                    r.setInvoice(res);
                    r.setStatus("INVOICED");
                    DataStore.save(requests);
                });

        return res;
    }

    // =========================
    // GET ALL
    // =========================
    @Override
    public List<RepairRequest> getAllRequests() {
        return requests;
    }

    // =========================
    // UPDATE STATUS
    // =========================
    @Override
    public void updateStatus(int index, String status) {
        requests.stream()
                .filter(r -> r.getId() == index)
                .findFirst()
                .ifPresent(r -> {
                    r.setStatus(status);
                    DataStore.save(requests);
                });
    }
}