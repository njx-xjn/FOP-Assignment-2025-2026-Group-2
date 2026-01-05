import java.util.*;
import java.time.LocalDate;

public class Analytics {
    private DataLoader dataLoader;

    public Analytics() {
        this.dataLoader = new DataLoader();
    }

    /**
     * Helper class to store performance data for the manager report.
     * Includes ID, Total Sales, and Transaction Count.
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
     * Calculates performance metrics per employee and sorts them descending (highest to lowest).
     * This will include the Manager if they have recorded sales.
     */
    public List<PerformanceEntry> getEmployeePerformance() {
        // Load all transactions using the dataLoader
        List<Transaction> allTxns = dataLoader.loadTransactions();
        Map<String, PerformanceEntry> metrics = new HashMap<>();

        for (Transaction t : allTxns) {
            // Process only 'SALE' type transactions for this report
            if (t.getType().equals("SALE")) {
                String id = t.getEmployeeId();
                
                // Group by Employee ID and sum totals
                metrics.putIfAbsent(id, new PerformanceEntry(id));
                PerformanceEntry entry = metrics.get(id);
                entry.totalSales += t.getTotalAmount();
                entry.transactionCount++;
            }
        }

        List<PerformanceEntry> sortedList = new ArrayList<>(metrics.values());
        
        // Manual Bubble Sort in descending order as per prerequisite
        int n = sortedList.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                // If current item is less than next item, swap them (Descending)
                if (sortedList.get(j).totalSales < sortedList.get(j + 1).totalSales) {
                    PerformanceEntry temp = sortedList.get(j);
                    sortedList.set(j, sortedList.get(j + 1));
                    sortedList.set(j + 1, temp);
                }
            }
        }
        return sortedList;
    }

    // --- RECENTLY ADDED FILTER/SORT FEATURES ---

    public List<Transaction> filterSalesByDate(List<Transaction> allTransactions, LocalDate start, LocalDate end) {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : allTransactions) {
            if (t.getType().equals("SALE")) {
                try {
                    LocalDate tDate = LocalDate.parse(t.getDate());
                    if (!tDate.isBefore(start) && !tDate.isAfter(end)) {
                        filtered.add(t);
                    }
                } catch (Exception e) {
                    // Ignore parsing errors for invalid dates
                }
            }
        }
        return filtered;
    }

    public double calculateCumulativeTotal(List<Transaction> filteredList) {
        return filteredList.stream().mapToDouble(Transaction::getTotalAmount).sum();
    }

    public void sortSales(List<Transaction> list, String criteria, boolean ascending) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                boolean swap = false;
                Transaction t1 = list.get(j);
                Transaction t2 = list.get(j + 1);
                switch (criteria) {
                    case "Date": 
                        swap = ascending ? t1.getDate().compareTo(t2.getDate()) > 0 : t1.getDate().compareTo(t2.getDate()) < 0; 
                        break;
                    case "Amount": 
                        swap = ascending ? t1.getTotalAmount() > t2.getTotalAmount() : t1.getTotalAmount() < t2.getTotalAmount(); 
                        break;
                    case "Customer": 
                        swap = ascending ? t1.getCustomerName().compareToIgnoreCase(t2.getCustomerName()) > 0 : t1.getCustomerName().compareToIgnoreCase(t2.getCustomerName()) < 0; 
                        break;
                }
                if (swap) {
                    list.set(j, t2);
                    list.set(j + 1, t1);
                }
            }
        }
    }

    // --- ORIGINAL FEATURES ---

    public double calculateTotalSales() {
        return dataLoader.loadTransactions().stream()
                .filter(t -> t.getType().equals("SALE"))
                .mapToDouble(Transaction::getTotalAmount)
                .sum();
    }

    public String getTopSellingModel() {
        Map<String, Integer> counts = new HashMap<>();
        for (Transaction t : dataLoader.loadTransactions()) {
            if (t.getType().equals("SALE")) {
                counts.put(t.getModelName(), counts.getOrDefault(t.getModelName(), 0) + t.getQuantity());
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
}