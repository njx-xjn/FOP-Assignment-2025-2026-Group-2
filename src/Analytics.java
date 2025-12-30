
import java.util.*;
import java.util.stream.Collectors;

public class Analytics {
    private DataLoader dataLoader;

    public Analytics() {
        this.dataLoader = new DataLoader();
    }

    public double calculateTotalSales() {
        List<Transaction> txns = dataLoader.loadTransactions();
        return txns.stream()
                .filter(t -> t.getType().equals("SALE"))
                .mapToDouble(Transaction::getTotalAmount)
                .sum();
    }

    public Map<String, Double> getSalesByEmployee() {
        List<Transaction> txns = dataLoader.loadTransactions();
        Map<String, Double> performance = new HashMap<>();

        for (Transaction t : txns) {
            if (t.getType().equals("SALE")) {
                performance.put(t.getEmployeeId(),
                        performance.getOrDefault(t.getEmployeeId(), 0.0) + t.getTotalAmount());
            }
        }
        return performance; // Returns Map<EmployeeID, TotalSales>
    }

    public String getTopSellingModel() {
        List<Transaction> txns = dataLoader.loadTransactions();
        Map<String, Integer> counts = new HashMap<>();

        for (Transaction t : txns) {
            if (t.getType().equals("SALE")) {
                counts.put(t.getModelName(),
                        counts.getOrDefault(t.getModelName(), 0) + t.getQuantity());
            }
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
}
