package server;

import common.InvoiceRequest;
import common.InvoiceResponse;
import common.RepairRequest;
import common.RepairService;

import javax.jws.WebService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@WebService(endpointInterface = "common.RepairService")
public class RepairServiceImpl implements RepairService {

    private static final AtomicInteger nextId = new AtomicInteger(0);
    private static List<RepairRequest> requests = initRequests();

    private static List<RepairRequest> initRequests() {
        List<RepairRequest> loaded = DataStore.load();
        int max = loaded.stream().mapToInt(RepairRequest::getId).max().orElse(-1);
        nextId.set(max + 1);
        return loaded;
    }

    @Override
    public synchronized RepairRequest sendRepairRequest(RepairRequest request) {
        request.setId(nextId.getAndIncrement());
        request.setStatus("NEW");
        requests.add(request);
        DataStore.save(requests);

        System.out.println("✔ NEW REQUEST #" + request.getId() +
                " from " + request.getClientName() +
                " (" + request.getDevice() + "), images=" +
                request.getImagesBase64().size());

        return request; // zwracamy całe zgłoszenie z ID i statusem
    }

    @Override
    public synchronized InvoiceResponse createInvoice(InvoiceRequest req) {
        InvoiceResponse res = new InvoiceResponse();
        res.setInvoiceId(UUID.randomUUID().toString());
        res.setActions(req.getActions());
        res.setLaborCost(req.getLaborCost());
        res.setPartsCost(req.getPartsCost());
        res.setPrice(req.getLaborCost() + req.getPartsCost());
        res.setStatus("SENT");

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

    @Override
    public synchronized List<RepairRequest> getAllRequests() {
        return requests;
    }

    @Override
    public synchronized void updateStatus(int id, String status) {
        requests.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .ifPresent(r -> {
                    r.setStatus(status);
                    DataStore.save(requests);
                });
    }
    @Override
    public synchronized List<RepairRequest> getAllRequestsLight() {
        // Zwraca kopie bez zdjęć - tylko metadane i faktura
        return requests.stream().map(r -> {
            RepairRequest light = new RepairRequest();
            light.setId(r.getId());
            light.setClientName(r.getClientName());
            light.setDevice(r.getDevice());
            light.setStatus(r.getStatus());
            light.setDescription(r.getDescription());
            light.setInvoice(r.getInvoice());
            light.setImagesBase64(new java.util.ArrayList<>()); // puste!
            return light;
        }).collect(java.util.stream.Collectors.toList());
    }
}
