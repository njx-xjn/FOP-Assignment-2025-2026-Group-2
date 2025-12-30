
import java.io.*;
import java.util.*;

public class DataLoader {
    private static final String MODEL_FILE = "model.csv";
    private static final String ATTENDANCE_FILE = "attendance.csv";
    // private static final String SALES_FILE = "sales_log.csv"; // DELETED

    // Load Models from CSV
    public Map<String, Model> loadModels() {
        Map<String, Model> models = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(MODEL_FILE))) {
            String header = br.readLine(); // Skip header
            String[] headers = header.split(","); // Get outlet codes from header

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length > 2) {
                    String name = data[0];
                    double price = Double.parseDouble(data[1]);
                    Model m = new Model(name, price);

                    // Outlets start from index 2
                    for (int i = 2; i < data.length; i++) {
                        if (i < headers.length) {
                            m.setStock(headers[i].trim(), Integer.parseInt(data[i]));
                        }
                    }
                    models.put(name, m);
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading models: " + e.getMessage());
        }
        return models;
    }

    // Save Models back to CSV
    public void saveModels(Map<String, Model> models, List<String> outletCodes) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MODEL_FILE))) {
            // Reconstruct Header
            StringBuilder header = new StringBuilder("Model,Price");
            for (String code : outletCodes) {
                header.append(",").append(code);
            }
            pw.println(header);

            for (Model m : models.values()) {
                StringBuilder row = new StringBuilder();
                row.append(m.getModelName()).append(",").append(m.getPrice());
                for (String code : outletCodes) {
                    row.append(",").append(m.getStock(code));
                }
                pw.println(row);
            }
        } catch (IOException e) {
            System.out.println("Error saving models: " + e.getMessage());
        }
    }

    // Log Transaction (Sales or Stock Move) - DEPRECATED / REMOVED CSV LOGGING
    public void logTransaction(Transaction t) {
        // No-op: Sales data is now only saved to receipt text files
    }

    // Load Sales History - DISABLED due to removal of CSV
    public List<Transaction> loadTransactions() {
        return new ArrayList<>(); // Return empty list
    }
    // --- ATTENDANCE SYSTEM ---

    // Clock In
    public String clockIn(String empId) {
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));

        // Check if already clocked in today to prevent duplicates (Simple check)
        List<String[]> lines = readCSV(ATTENDANCE_FILE);
        for (String[] row : lines) {
            if (row.length > 0 && row[0].equals(empId) && row[1].equals(date) && (row.length < 4 || row[3].isEmpty())) {
                return "Already Clocked In for today!";
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ATTENDANCE_FILE, true))) {
            // ID,Date,InTime,OutTime,Total
            bw.write(empId + "," + date + "," + time + ",,");
            bw.newLine();
            return "Clock In Successful!\nDate: " + date + "\nTime: " + time;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    // Clock Out
    public String clockOut(String empId) {
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        List<String[]> lines = readCSV(ATTENDANCE_FILE);
        boolean found = false;
        String result = "";

        List<String> newLines = new ArrayList<>();
        newLines.add("EmployeeID,Date,ClockInTime,ClockOutTime,TotalHours"); // Header

        for (int i = 1; i < lines.size(); i++) { // Skip header in read
            String[] row = lines.get(i);
            if (!found && row.length > 0 && row[0].equals(empId) && row[1].equals(date)
                    && (row.length < 4 || row[3].isEmpty())) {
                // Found the open entry
                found = true;
                String inTimeStr = row[2];

                // Calculate Duration
                try {
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                    java.time.LocalTime inTime = java.time.LocalTime.parse(inTimeStr, fmt);
                    java.time.LocalTime outTime = java.time.LocalTime.parse(time, fmt);
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(inTime, outTime);
                    double hours = minutes / 60.0;

                    String totalHours = String.format("%.1f hours", hours);

                    // Reconstruct line with OutTime and Total
                    String newLine = row[0] + "," + row[1] + "," + row[2] + "," + time + "," + totalHours;
                    newLines.add(newLine);

                    result = "Clock Out Successful!\nDate: " + date + "\nTime: " + time + "\nTotal Hours Worked: "
                            + totalHours;

                } catch (Exception e) {
                    newLines.add(String.join(",", row)); // Error case, keep as is
                    result = "Error calculating time: " + e.getMessage();
                }
            } else {
                newLines.add(String.join(",", row));
            }
        }

        if (!found)
            return "Error: You have not clocked in today!";

        // Write back
        try (PrintWriter pw = new PrintWriter(new FileWriter(ATTENDANCE_FILE))) {
            for (String line : newLines) {
                pw.println(line);
            }
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }

        return result;
    }

    private List<String[]> readCSV(String file) {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.split(",")); // Keep empty trailing strings? split limit -1
            }
        } catch (IOException e) {
        }
        return lines;
    }

    // --- RECEIPT LOGGING ---
    public void appendReceipt(String content) {
        String directoryName = "StockReceipt";
        File directory = new File(directoryName);
        if (!directory.exists()) {
            boolean created = directory.mkdir();
            if (!created) {
                System.out.println("Error: Could not create directory " + directoryName);
            }
        }

        String date = java.time.LocalDate.now().toString();
        // Use File.separator for cross-platform compatibility, though likely running on
        // Windows here
        String filename = directoryName + File.separator + "receipts_" + date + ".txt";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) { // Append mode
            bw.write(content);
            bw.newLine();
            bw.write("--------------------------------------------------");
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving receipt: " + e.getMessage());
        }
    }

    // --- SALES RECEIPT LOGGING ---
    public void appendSalesReceipt(String content) {
        String directoryName = "SalesReceipt";
        File directory = new File(directoryName);
        if (!directory.exists()) {
            boolean created = directory.mkdir();
            if (!created) {
                System.out.println("Error: Could not create directory " + directoryName);
            }
        }

        String date = java.time.LocalDate.now().toString();
        String filename = directoryName + File.separator + "sales_" + date + ".txt";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) { // Append mode
            bw.write(content);
            bw.newLine();
            bw.write("--------------------------------------------------");
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving sales receipt: " + e.getMessage());
        }
    }

    // --- SEARCH SALES RECEIPTS ---
    public String searchSalesReceipts(String keyword) {
        File dir = new File("SalesReceipt");
        if (!dir.exists() || !dir.isDirectory()) {
            return "No sales records found (Folder missing).";
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            return "No sales records found.";
        }

        StringBuilder results = new StringBuilder();
        int matches = 0;
        String lowerKey = keyword.toLowerCase();

        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                List<String> block = new ArrayList<>();
                boolean blockHasMatch = false;

                while ((line = br.readLine()) != null) {
                    if (line.startsWith("===") && !block.isEmpty()) {
                        // Process previous block
                        if (blockHasMatch) {
                            for (String l : block)
                                results.append(l).append("\n");
                            results.append("--------------------\n");
                            matches++;
                            blockHasMatch = false;
                        }
                        block.clear();
                    }

                    block.add(line);
                    if (line.toLowerCase().contains(lowerKey)) {
                        blockHasMatch = true;
                    }
                }
                // Check last block
                if (blockHasMatch) {
                    for (String l : block)
                        results.append(l).append("\n");
                    results.append("--------------------\n");
                    matches++;
                }

            } catch (IOException e) {
                results.append("Error reading file: ").append(f.getName()).append("\n");
            }
        }

        if (matches == 0) {
            return "No matches found for '" + keyword + "'.";
        }
        return results.toString();
    }

}
