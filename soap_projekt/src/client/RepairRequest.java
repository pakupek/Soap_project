package client;

import java.util.List;

public class RepairRequest {
    private String clientName;
    private String device;
    private String description;
    private List<String> imagesBase64;
    private String status;

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getImagesBase64() { return imagesBase64; }
    public void setImagesBase64(List<String> imagesBase64) { this.imagesBase64 = imagesBase64; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}