package common;

public class InvoiceRequest {
    private int requestId;
    private String actions;
    private double laborCost;
    private double partsCost;

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }

    public double getLaborCost() { return laborCost; }
    public void setLaborCost(double laborCost) { this.laborCost = laborCost; }

    public double getPartsCost() { return partsCost; }
    public void setPartsCost(double partsCost) { this.partsCost = partsCost; }

    public double getTotal() {
        return laborCost + partsCost;
    }
}