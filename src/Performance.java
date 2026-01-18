import java.util.*;

// LOGIC CLASS: PERFORMANCE CALCULATOR
// This class is used by 'PerformancePanel.java' (The Manager's Tab).
// It processes the raw transaction list to figure out who sold the most.
public class Performance {
    private DataLoader dataLoader;

    public Performance() {
        this.dataLoader = new DataLoader();
    }

    /**
     * Helper class (Data Structure) to store the score for one employee.
     * Think of this like a temporary "Scorecard".
     */
    public static class PerformanceEntry {
        public String empId;
        public double totalSales;      // How much money they made (RM)
        public int transactionCount;   // How many times they pressed "Pay"

        public PerformanceEntry(String empId) {
            this.empId = empId;
            this.totalSales = 0;
            this.transactionCount = 0;
        }
    }

    /**
     * THE MAIN LOGIC
     * 1. Loads all sales history.
     * 2. Groups sales by Employee ID.
     * 3. Sorts them so the #1 Seller is at the top.
     */
    public List<PerformanceEntry> getEmployeePerformance() {
        // Step 1: Load every single transaction from the text files
        List<Transaction> allTxns = dataLoader.loadTransactions();
        
        // Step 2: Aggregate (Group By Employee)
        // Map Key = Employee ID (e.g., "001")
        // Map Value = Their Scorecard (PerformanceEntry)
        Map<String, PerformanceEntry> metrics = new HashMap<>();

        for (Transaction t : allTxns) {
            // Only count actual SALES (ignore returns/refunds if any)
            if (t.getType().equals("SALE")) {
                String id = t.getEmployeeId();
                
                // If this is the first time seeing this ID, create a blank scorecard
                metrics.putIfAbsent(id, new PerformanceEntry(id));
                
                // Add this specific sale to their total score
                PerformanceEntry entry = metrics.get(id);
                entry.totalSales += t.getTotalAmount();
                entry.transactionCount++;
            }
        }

        // Convert the Map to a List so we can sort it
        List<PerformanceEntry> sortedList = new ArrayList<>(metrics.values());

        // Step 3: Sort (Descending Order - Highest Sales First)
        // Using Bubble Sort algorithm
        int n = sortedList.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                // Compare: If Person J made LESS than Person J+1...
                if (sortedList.get(j).totalSales < sortedList.get(j + 1).totalSales) {
                    // ... SWAP them (push the lower value down)
                    PerformanceEntry temp = sortedList.get(j);
                    sortedList.set(j, sortedList.get(j + 1));
                    sortedList.set(j + 1, temp);
                }
            }
        }
        
        return sortedList; // Return the final leaderboard
    }
}