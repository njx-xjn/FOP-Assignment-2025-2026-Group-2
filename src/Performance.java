import java.util.*;

public class Performance {
    private DataLoader dataLoader;

    public Performance() {
        this.dataLoader = new DataLoader();
    }

    /**
     * Helper class to store performance data for the manager report.
     */
    public static class PerformanceEntry {
        public String empId;
        public double totalSales;
        public int transactionCount;

        public PerformanceEntry(String empId) {
            this.empId = empId;
            this.totalSales = 0;
            this.transactionCount = 0;
        }
    }

    /**
     * Calculates performance metrics per employee and sorts them descending.
     */
    public List<PerformanceEntry> getEmployeePerformance() {
        // Load all transactions using the dataLoader
        List<Transaction> allTxns = dataLoader.loadTransactions();
        Map<String, PerformanceEntry> metrics = new HashMap<>();

        for (Transaction t : allTxns) {
            if (t.getType().equals("SALE")) {
                String id = t.getEmployeeId();
                metrics.putIfAbsent(id, new PerformanceEntry(id));
                PerformanceEntry entry = metrics.get(id);
                entry.totalSales += t.getTotalAmount();
                entry.transactionCount++;
            }
        }

        List<PerformanceEntry> sortedList = new ArrayList<>(metrics.values());

        // Manual Bubble Sort (Descending)
        int n = sortedList.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (sortedList.get(j).totalSales < sortedList.get(j + 1).totalSales) {
                    PerformanceEntry temp = sortedList.get(j);
                    sortedList.set(j, sortedList.get(j + 1));
                    sortedList.set(j + 1, temp);
                }
            }
        }
        return sortedList;
    }
}
