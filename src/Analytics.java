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
     * Calculates performance metrics per employee and sorts them descending
     * (highest to lowest).
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
                        swap = ascending ? t1.getDate().compareTo(t2.getDate()) > 0
                                : t1.getDate().compareTo(t2.getDate()) < 0;
                        break;
                    case "Amount":
                        swap = ascending ? t1.getTotalAmount() > t2.getTotalAmount()
                                : t1.getTotalAmount() < t2.getTotalAmount();
                        break;
                    case "Customer":
                        swap = ascending ? t1.getCustomerName().compareToIgnoreCase(t2.getCustomerName()) > 0
                                : t1.getCustomerName().compareToIgnoreCase(t2.getCustomerName()) < 0;
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

    public String getTopSellingModelForList(List<Transaction> transactions) {
        Map<String, Integer> counts = new HashMap<>();
        for (Transaction t : transactions) {
            counts.put(t.getModelName(), counts.getOrDefault(t.getModelName(), 0) + t.getQuantity());
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    // --- NEW HELPERS FOR ANALYTICS TAB ---

    public LocalDate getStartDateForPeriod(String period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case "Today":
                return now;
            case "This Week":
                return now.minusDays(6); // Last 7 days including today
            case "This Month":
                return now.withDayOfMonth(1);
            default:
                return now;
        }
    }

    public Map<String, Double> getTrendData(List<Transaction> transactions, String period) {
        Map<String, Double> trend = new LinkedHashMap<>();

        // Initialize placeholders based on period
        if (period.equals("Today")) {
            // For Today: Show 24-hour hourly buckets
            for (int i = 0; i < 24; i++) {
                trend.put(String.format("%02d:00", i), 0.0);
            }
        } else if (period.equals("This Week")) {
            // For Week: Show last 7 days
            LocalDate start = LocalDate.now().minusDays(6);
            for (int i = 0; i < 7; i++) {
                trend.put(start.plusDays(i).toString(), 0.0);
            }
        } else if (period.equals("This Month")) {
            // For Month: Show weeks (Week 1, Week 2...) or just days?
            // Let's do days for better granularity
            int len = LocalDate.now().lengthOfMonth();
            for (int i = 0; i < len; i++) {
                trend.put(String.format("%d", i + 1), 0.0);
            }
        }

        // Fill data
        for (Transaction t : transactions) {
            String key = "";
            if (period.equals("Today")) {
                // Key is Hour
                try {
                    // Assuming time format is "hh:mm a" e.g., "02:30 PM"
                    // We need to convert it to 24h format for sorting/bucketing if simple string
                    // match
                    // But simpler is to allow flexible keys.
                    // Let's just grab the hour prefix logic or rely on Transaction time parsing?
                    // Simplest:
                    String time = t.getTime(); // "hh:mm a"
                    // Parse to LocalTime to get hour
                    java.time.LocalTime lt = java.time.LocalTime.parse(time,
                            java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
                    key = String.format("%02d:00", lt.getHour());
                } catch (Exception e) {
                }
            } else if (period.equals("This Week")) {
                key = t.getDate(); // YYYY-MM-DD
            } else if (period.equals("This Month")) {
                key = String.valueOf(LocalDate.parse(t.getDate()).getDayOfMonth());
            }

            if (trend.containsKey(key)) {
                trend.put(key, trend.get(key) + t.getTotalAmount());
            }
        }
        return trend;
    }

    public double calculateAverageDailySales(List<Transaction> transactions, String period) {
        double total = calculateCumulativeTotal(transactions);
        int days = 1;
        if (period.equals("This Week"))
            days = 7;
        if (period.equals("This Month"))
            days = LocalDate.now().getDayOfMonth(); // Average over days passed so far
        return (days > 0) ? total / days : 0;
    }
}