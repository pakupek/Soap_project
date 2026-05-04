package common;

public class InvoiceResponse {

    private String invoiceId;

    // 🔥 dane od admina
    private String actions;
    private double laborCost;
    private double partsCost;

    // 🔥 wynik
    private double price;
    private String status;

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }

    public double getLaborCost() { return laborCost; }
    public void setLaborCost(double laborCost) { this.laborCost = laborCost; }

    public double getPartsCost() { return partsCost; }
    public void setPartsCost(double partsCost) { this.partsCost = partsCost; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}