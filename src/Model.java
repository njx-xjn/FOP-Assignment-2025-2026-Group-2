
import java.util.HashMap;
import java.util.Map;

public class Model {
    private String modelName;
    private double price;
    private Map<String, Integer> stockPerOutlet; // OutletCode -> Quantity

    public Model(String modelName, double price) {
        this.modelName = modelName;
        this.price = price;
        this.stockPerOutlet = new HashMap<>();
    }

    public String getModelName() {
        return modelName;
    }

    public double getPrice() {
        return price;
    }

    public void setStock(String outletCode, int quantity) {
        stockPerOutlet.put(outletCode, quantity);
    }

    public int getStock(String outletCode) {
        return stockPerOutlet.getOrDefault(outletCode, 0);
    }

    public void addStock(String outletCode, int quantity) {
        stockPerOutlet.put(outletCode, getStock(outletCode) + quantity);
    }

    public void reduceStock(String outletCode, int quantity) {
        int current = getStock(outletCode);
        if (current >= quantity) {
            stockPerOutlet.put(outletCode, current - quantity);
        } else {
            throw new IllegalArgumentException("Insufficient stock for " + modelName + " at " + outletCode);
        }
    }

    public Map<String, Integer> getAllStock() {
        return stockPerOutlet;
    }

    public Map<String, Integer> getStocks() {
        return stockPerOutlet;
    }

    public int getTotalStock() {
        int total = 0;
        for (int qty : stockPerOutlet.values()) {
            total += qty;
        }
        return total;
    }
}
