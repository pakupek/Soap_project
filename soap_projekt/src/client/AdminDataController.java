package client;

import common.InvoiceRequest;
import common.InvoiceResponse;
import common.RepairRequest;
import common.RepairService;

import java.util.List;

public class AdminDataController {

    private final RepairService service;

    public AdminDataController(RepairService service) {
        this.service = service;
    }

    public List<RepairRequest> getAll() {
        return service.getAllRequests();
    }

    public void updateStatus(int id, String status) {
        service.updateStatus(id, status);
    }

    public InvoiceResponse createInvoice(InvoiceRequest req) {
        return service.createInvoice(req);
    }
}