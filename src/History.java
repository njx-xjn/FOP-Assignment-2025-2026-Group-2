import java.util.*;
import java.time.LocalDate;

// LOGIC CLASS: HISTORY MANAGER
// This class is a "Helper". It doesn't store data permanently.
// It takes a list of transactions and performs operations like Filtering and Sorting.
// Used by: HistoryPanel.java and AnalyticsPanel.java
public class History {

    // --- FILTER LOGIC ---
    // This takes the massive list of ALL transactions and returns a smaller list
    // containing only the sales that happened between "Start Date" and "End Date".
    public List<Transaction> filterSalesByDate(List<Transaction> allTransactions, LocalDate start, LocalDate end) {
        List<Transaction> filtered = new ArrayList<>();
        
        for (Transaction t : allTransactions) {
            // Step 1: Only look at SALES (ignore "Restock" or "Return" entries if you have them)
            if (t.getType().equals("SALE")) {
                try {
                    // Step 2: Convert the String date from file to a Date object
                    LocalDate tDate = LocalDate.parse(t.getDate());
                    
                    // Step 3: Check if the date is INSIDE the range.
                    // !isBefore means "On or After"
                    // !isAfter means "On or Before"
                    // This creates an "Inclusive" range (e.g., from Jan 1 to Jan 3 includes Jan 1, 2, and 3).
                    if (!tDate.isBefore(start) && !tDate.isAfter(end)) {
                        filtered.add(t);
                    }
                } catch (Exception e) {
                    // If a date in the file is corrupt/unreadable, just skip that one item.
                    // We don't want to crash the whole app for one bad line.
                }
            }
        }
        return filtered;
    }

    // --- SORT LOGIC (Bubble Sort) ---
    // This rearranges the list based on what column header the user clicked.
    // We use a manual Bubble Sort algorithm here (Standard CS algorithm).
    public void sortSales(List<Transaction> list, String criteria, boolean ascending) {
        int n = list.size();
        
        // Nested loops to compare every item against every other item
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                boolean swap = false;
                Transaction t1 = list.get(j);
                Transaction t2 = list.get(j + 1);
                
                // Decide HOW to compare based on what the user wants to sort by
                switch (criteria) {
                    case "Date":
                        // Compare Date objects
                        swap = ascending ? t1.getDate().compareTo(t2.getDate()) > 0
                                : t1.getDate().compareTo(t2.getDate()) < 0;
                        break;
                    case "Amount":
                        // Compare Money (Double)
                        swap = ascending ? t1.getTotalAmount() > t2.getTotalAmount()
                                : t1.getTotalAmount() < t2.getTotalAmount();
                        break;
                    case "Customer":
                        // Compare String Names (A-Z)
                        // compareToIgnoreCase ensures "apple" and "Apple" are treated the same
                        swap = ascending ? t1.getCustomerName().compareToIgnoreCase(t2.getCustomerName()) > 0
                                : t1.getCustomerName().compareToIgnoreCase(t2.getCustomerName()) < 0;
                        break;
                }
                
                // If the items are in the wrong order, SWAP them.
                if (swap) {
                    list.set(j, t2);
                    list.set(j + 1, t1);
                }
            }
        }
    }
}