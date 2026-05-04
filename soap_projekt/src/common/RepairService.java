package common;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.util.List;

@WebService
public interface RepairService {
    @WebMethod
    RepairRequest sendRepairRequest(RepairRequest request);

    @WebMethod
    List<RepairRequest> getAllRequests();

    @WebMethod
    void updateStatus(int index, String status);

    @WebMethod
    InvoiceResponse createInvoice(InvoiceRequest request);
}