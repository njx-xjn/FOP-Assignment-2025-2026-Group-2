import java.util.*;
import java.time.LocalDate;

public class History {

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
                    // Ignore parsing errors
                }
            }
        }
        return filtered;
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
}
