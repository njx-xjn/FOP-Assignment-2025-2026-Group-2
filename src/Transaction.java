
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private String transactionId;
    private String type; // "SALE", "STOCK_IN", "STOCK_OUT"
    private String date;
    private String time;
    private String employeeId;
    private String modelName;
    private int quantity;
    private String outletCode; // Where the transaction happened
    private String customerName; // Only for SALE
    private double totalAmount; // Only for SALE

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

    // Constructor for SALE
    public Transaction(String employeeId, String outletCode, String modelName, int quantity, double totalAmount,
            String customerName) {
        this.type = "SALE";
        this.employeeId = employeeId;
        this.outletCode = outletCode;
        this.modelName = modelName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.customerName = customerName;
        this.date = LocalDateTime.now().format(dateFormatter);
        this.time = LocalDateTime.now().format(timeFormatter);
        this.transactionId = "TXN-" + System.currentTimeMillis();
    }

    // Constructor for STOCK IN/OUT
    public Transaction(String type, String employeeId, String outletCode, String modelName, int quantity) {
        this.type = type;
        this.employeeId = employeeId;
        this.outletCode = outletCode;
        this.modelName = modelName;
        this.quantity = quantity;
        this.date = LocalDateTime.now().format(dateFormatter);
        this.time = LocalDateTime.now().format(timeFormatter);
        this.transactionId = "STK-" + System.currentTimeMillis();
        this.customerName = "N/A";
        this.totalAmount = 0.0;
    }

    // Getters
    public String getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getModelName() {
        return modelName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getOutletCode() {
        return outletCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    // For CSV reconstruction
    public Transaction(String id, String type, String date, String time, String empId, String outlet, String model,
            int qty, double amt, String cust) {
        this.transactionId = id;
        this.type = type;
        this.date = date;
        this.time = time;
        this.employeeId = empId;
        this.outletCode = outlet;
        this.modelName = model;
        this.quantity = qty;
        this.totalAmount = amt;
        this.customerName = cust;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%d,%.2f,%s",
                transactionId, type, date, time, employeeId, outletCode, modelName, quantity, totalAmount,
                customerName);
    }
}
