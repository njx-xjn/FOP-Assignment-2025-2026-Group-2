import java.util.*;
import java.time.LocalDate;

// THE ANALYTICS ENGINE
// This class is the "brain" of the dashboard. It takes raw lists of transactions
// and calculates the numbers needed for charts, graphs, and total cards.
public class Analytics {
    private DataLoader dataLoader;

    // Constructor: This runs once when the app starts.
    // It prepares the 'dataLoader' so we can fetch data from the file system or database.
    public Analytics() {
        this.dataLoader = new DataLoader();
    }

    // --- DASHBOARD TOTALS (The numbers at the top of the screen) ---

    // 1. Calculate Grand Total Sales
    // This goes through the entire database to find how much money we made.
    public double calculateTotalSales() {
        return dataLoader.loadTransactions().stream()
                // Step 1: Filter. Imagine a conveyor belt of transactions.
                // We pick up each one and ask: "Is this a SALE?" 
                // We throw away "REFUND" or "RETURN" types here.
                .filter(t -> t.getType().equals("SALE")) 
                
                // Step 2: Extract Price. Now we only look at the money amount ($) 
                // of that sale, ignoring the date, model name, etc.
                .mapToDouble(Transaction::getTotalAmount) 
                
                // Step 3: Sum. Add all those numbers together.
                .sum(); 
    }

    // 2. Sum up a specific list
    // Unlike the method above (which loads everything), this calculates the total 
    // for a specific list you pass to it (e.g., just "Today's" transactions).
    public double calculateCumulativeTotal(List<Transaction> filteredList) {
        return filteredList.stream().mapToDouble(Transaction::getTotalAmount).sum();
    }

    // 3. Find the "Best Seller"
    // This looks at a list of sales and figures out which Item Model appears the most.
    public String getTopSellingModelForList(List<Transaction> transactions) {
        // Create a blank "Scoreboard" (Map) to keep count.
        // Key = Model Name (e.g., "iPhone"), Value = Count (e.g., 5)
        Map<String, Integer> counts = new HashMap<>();
        
        // Loop through every single transaction in the list
        for (Transaction t : transactions) {
            // Update the scoreboard:
            // Get the current count for this model (or 0 if it's new).
            // Add the quantity sold in this transaction to the count.
            counts.put(t.getModelName(), counts.getOrDefault(t.getModelName(), 0) + t.getQuantity());
        }
        
        // Now look at the scoreboard to find the winner.
        return counts.entrySet().stream()
                // Find the entry with the highest Value (highest count)
                .max(Map.Entry.comparingByValue())
                // Get the Key (the Model Name) of that winner
                .map(Map.Entry::getKey) 
                // If the list was empty (no sales), return "N/A" so the app doesn't crash.
                .orElse("N/A"); 
    }

    // 4. Calculate Average Sales Per Day
    // Logic: Total Money / Number of Days
    public double calculateAverageDailySales(List<Transaction> transactions, String period) {
        double total = calculateCumulativeTotal(transactions);
        int days = 1; // Start with 1 to prevent "Division by Zero" errors if data is missing
        
        // If the user selected "This Week", we divide by 7 days.
        if (period.equals("This Week"))
            days = 7;
        
        // If "This Month", we divide by today's date (e.g., if it's the 15th, divide by 15).
        // This gives us the average "So Far" this month.
        if (period.equals("This Month"))
            days = LocalDate.now().getDayOfMonth(); 
        
        return (days > 0) ? total / days : 0;
    }

    // --- TRENDS & GRAPHS (The Chart Data) ---

    // Helper: Figure out the specific date to start looking for data
    public LocalDate getStartDateForPeriod(String period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case "Today":
                return now;
            case "This Week":
                return now.minusDays(6); // Go back 6 days (Today + 6 days back = 7 days total)
            case "This Month":
                return now.withDayOfMonth(1); // Reset date to the 1st of the current month
            default:
                return now;
        }
    }

    // THE COMPLEX PART: PREPARING GRAPH DATA
    // This creates a list of "Buckets" (Time periods) and fills them with money amounts.
    public Map<String, Double> getTrendData(List<Transaction> transactions, String period) {
        // We use LinkedHashMap because it remembers the order. 
        // (We want the graph to go 1pm, 2pm, 3pm... not random order).
        Map<String, Double> trend = new LinkedHashMap<>();

        // --- STEP 1: CREATE EMPTY BUCKETS ---
        // Even if we had $0 sales at 2:00 PM, the graph still needs a spot for "2:00 PM".
        
        if (period.equals("Today")) {
            // Create 24 buckets: "00:00", "01:00" ... up to "23:00"
            for (int i = 0; i < 24; i++) {
                trend.put(String.format("%02d:00", i), 0.0);
            }
        } else if (period.equals("This Week")) {
            // Create buckets for the last 7 dates.
            LocalDate start = LocalDate.now().minusDays(6);
            for (int i = 0; i < 7; i++) {
                trend.put(start.plusDays(i).toString(), 0.0);
            }
        } else if (period.equals("This Month")) {
            // Create buckets for every day of the current month (1st to 30th/31st)
            int len = LocalDate.now().lengthOfMonth();
            for (int i = 0; i < len; i++) {
                trend.put(String.format("%d", i + 1), 0.0);
            }
        }

        // --- STEP 2: FILL THE BUCKETS ---
        // Now look at the actual transactions and drop the money into the correct bucket.
        
        for (Transaction t : transactions) {
            String key = ""; // This will hold the label of the bucket we want (e.g., "14:00")
            
            // LOGIC FOR MATCHING TRANSACTIONS TO BUCKETS
            if (period.equals("Today")) {
                try {
                    // The transaction has a time like "02:30 PM". We need to turn that into "14:00".
                    String time = t.getTime(); 
                    java.time.LocalTime lt = java.time.LocalTime.parse(time,
                            java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
                    // Take the hour and format it (e.g., 14 becomes "14:00")
                    key = String.format("%02d:00", lt.getHour());
                } catch (Exception e) {
                    // If the time format is broken, skip this transaction safely.
                }
            } else if (period.equals("This Week")) {
                // If viewing weekly, the bucket label is just the Date (e.g., "2023-10-25")
                key = t.getDate();
            } else if (period.equals("This Month")) {
                // If viewing monthly, the bucket label is the Day Number (e.g., "25")
                key = String.valueOf(LocalDate.parse(t.getDate()).getDayOfMonth());
            }

            // --- STEP 3: ADD TO TOTAL ---
            // If we found a matching bucket for this transaction...
            if (trend.containsKey(key)) {
                // Take the money already in the bucket + the new money from this transaction
                trend.put(key, trend.get(key) + t.getTotalAmount());
            }
        }
        return trend; // Return the filled buckets to be drawn on the screen
    }
}