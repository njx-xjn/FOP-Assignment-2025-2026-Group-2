import java.util.*;
import java.time.LocalDate;

public class Analytics {
    private DataLoader dataLoader;

    public Analytics() {
        this.dataLoader = new DataLoader();
    }

    // --- DASHBOARD TOTALS ---

    public double calculateTotalSales() {
        return dataLoader.loadTransactions().stream()
                .filter(t -> t.getType().equals("SALE"))
                .mapToDouble(Transaction::getTotalAmount)
                .sum();
    }

    public double calculateCumulativeTotal(List<Transaction> filteredList) {
        return filteredList.stream().mapToDouble(Transaction::getTotalAmount).sum();
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

    public double calculateAverageDailySales(List<Transaction> transactions, String period) {
        double total = calculateCumulativeTotal(transactions);
        int days = 1;
        if (period.equals("This Week"))
            days = 7;
        if (period.equals("This Month"))
            days = LocalDate.now().getDayOfMonth(); 
        return (days > 0) ? total / days : 0;
    }

    // --- TRENDS & GRAPHS ---

    public LocalDate getStartDateForPeriod(String period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case "Today":
                return now;
            case "This Week":
                return now.minusDays(6);
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
            for (int i = 0; i < 24; i++) {
                trend.put(String.format("%02d:00", i), 0.0);
            }
        } else if (period.equals("This Week")) {
            LocalDate start = LocalDate.now().minusDays(6);
            for (int i = 0; i < 7; i++) {
                trend.put(start.plusDays(i).toString(), 0.0);
            }
        } else if (period.equals("This Month")) {
            int len = LocalDate.now().lengthOfMonth();
            for (int i = 0; i < len; i++) {
                trend.put(String.format("%d", i + 1), 0.0);
            }
        }

        // Fill data
        for (Transaction t : transactions) {
            String key = "";
            if (period.equals("Today")) {
                try {
                    String time = t.getTime(); 
                    java.time.LocalTime lt = java.time.LocalTime.parse(time,
                            java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
                    key = String.format("%02d:00", lt.getHour());
                } catch (Exception e) {
                }
            } else if (period.equals("This Week")) {
                key = t.getDate();
            } else if (period.equals("This Month")) {
                key = String.valueOf(LocalDate.parse(t.getDate()).getDayOfMonth());
            }

            if (trend.containsKey(key)) {
                trend.put(key, trend.get(key) + t.getTotalAmount());
            }
        }
        return trend;
    }
}