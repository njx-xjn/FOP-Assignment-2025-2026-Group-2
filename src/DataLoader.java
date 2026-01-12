import java.io.*;
import java.util.*;

public class DataLoader {
    private static final String MODEL_FILE = "model.csv";
    private static final String ATTENDANCE_FILE = "attendance.csv";

    // Load Models from CSV
    public Map<String, Model> loadModels() {
        Map<String, Model> models = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(MODEL_FILE))) {
            String header = br.readLine();
            String[] headers = header.split(",");
            
            // Inside loadModels()
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim(); // Force remove hidden spaces
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length > 2) {
                    String name = data[0];
                    double price = Double.parseDouble(data[1]);
                    Model m = new Model(name, price);

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

    public void logTransaction(Transaction t) {
        // No-op: Sales data is now only saved to receipt text files
    }

    // --- UPDATED: Load Sales History for Performance Metrics (Capturing Employee
    // ID) ---
    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        File dir = new File("SalesReceipt");

        if (!dir.exists() || !dir.isDirectory()) {
            return transactions;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null)
            return transactions;

        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                String date = "", time = "", cust = "", model = "", empId = "N/A";
                double amt = 0;
                int qty = 0;

                while ((line = br.readLine()) != null) {
                    // Extracting details from the receipt format
                    if (line.contains("Date: "))
                        date = line.split(": ")[1].trim();
                    else if (line.contains("Time: "))
                        time = line.split(": ")[1].trim();

                    // MODIFIED: Capture the Employee ID to allow performance grouping (Critical for
                    // Tan Guan Han/C6001)
                    else if (line.contains("Employee: "))
                        empId = line.split(": ")[1].trim();

                    else if (line.contains("Customer Name: "))
                        cust = line.split(": ")[1].trim();
                    else if (line.contains("Model: "))
                        model = line.split(": ")[1].trim();
                    else if (line.contains("Quantity: "))
                        qty = Integer.parseInt(line.split(": ")[1].trim());
                    else if (line.contains("Subtotal: RM")) {
                        amt = Double.parseDouble(line.split("RM")[1].trim());
                    }

                    // Finalize transaction object at the separator
                    // Check for the long separator that marks the end of a receipt (length 50)
                    // The shorter separator (length 29) divides items from payment info
                    else if (line.startsWith("----------------") && line.length() > 40) {
                        if (!model.isEmpty()) {
                            // Transaction ID is empty string as per your request
                            // empId is included for the Manager Performance Metrics logic
                            transactions.add(new Transaction("",
                                    "SALE", date, time, empId, "C60", model, qty, amt, cust));
                        }
                        // Reset all fields for the next transaction in the file
                        model = "";
                        amt = 0;
                        qty = 0;
                        cust = "";
                        // Date/Time/EmpId likely persist for the file or are overwritten by next header
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing receipt " + f.getName() + ": " + e.getMessage());
            }
        }
        return transactions;
    }

    // --- ATTENDANCE SYSTEM ---
    public String clockIn(String empId) {
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        List<String[]> lines = readCSV(ATTENDANCE_FILE);
        for (String[] row : lines) {
            if (row.length > 0 && row[0].equals(empId) && row[1].equals(date) && (row.length < 4 || row[3].isEmpty())) {
                return "Already Clocked In for today!";
            }
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ATTENDANCE_FILE, true))) {
            bw.write(empId + "," + date + "," + time + ",,");
            bw.newLine();
            return "Clock In Successful!\nDate: " + date + "\nTime: " + time;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String clockOut(String empId) {
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        List<String[]> lines = readCSV(ATTENDANCE_FILE);
        boolean found = false;
        String result = "";
        List<String> newLines = new ArrayList<>();
        newLines.add("EmployeeID,Date,ClockInTime,ClockOutTime,TotalHours");

        for (int i = 1; i < lines.size(); i++) {
            String[] row = lines.get(i);
            if (!found && row.length > 0 && row[0].equals(empId) && row[1].equals(date)
                    && (row.length < 4 || row[3].isEmpty())) {
                found = true;
                try {
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                    java.time.LocalTime inTime = java.time.LocalTime.parse(row[2], fmt);
                    java.time.LocalTime outTime = java.time.LocalTime.parse(time, fmt);
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(inTime, outTime);
                    String totalHours = String.format("%.1f hours", minutes / 60.0);
                    newLines.add(row[0] + "," + row[1] + "," + row[2] + "," + time + "," + totalHours);
                    result = "Clock Out Successful!\nDate: " + date + "\nTime: " + time + "\nTotal Hours: "
                            + totalHours;
                } catch (Exception e) {
                    newLines.add(String.join(",", row));
                    result = "Error: " + e.getMessage();
                }
            } else {
                newLines.add(String.join(",", row));
            }
        }
        if (!found)
            return "Error: You have not clocked in today!";
        try (PrintWriter pw = new PrintWriter(new FileWriter(ATTENDANCE_FILE))) {
            for (String line : newLines)
                pw.println(line);
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
        return result;
    }

    private List<String[]> readCSV(String file) {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null)
                lines.add(line.split(","));
        } catch (IOException e) {
        }
        return lines;
    }

    public void appendReceipt(String content) {
        String directoryName = "StockReceipt";
        File directory = new File(directoryName);
        if (!directory.exists())
            directory.mkdir();
        String date = java.time.LocalDate.now().toString();
        String filename = directoryName + File.separator + "receipts_" + date + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            bw.write(content);
            bw.newLine();
            bw.write("--------------------------------------------------");
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving receipt: " + e.getMessage());
        }
    }

    public void appendSalesReceipt(String content) {
        String directoryName = "SalesReceipt";
        File directory = new File(directoryName);
        if (!directory.exists())
            directory.mkdir();
        String date = java.time.LocalDate.now().toString();
        String filename = directoryName + File.separator + "sales_" + date + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            bw.write(content);
            bw.newLine();
            bw.write("--------------------------------------------------");
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving sales receipt: " + e.getMessage());
        }
    }

    public String searchSalesReceipts(String keyword) {
        File dir = new File("SalesReceipt");
        if (!dir.exists() || !dir.isDirectory())
            return "No records found.";
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0)
            return "No records found.";

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
                        if (blockHasMatch) {
                            for (String l : block)
                                results.append(l).append("\n");
                            results.append("--------------------\n");
                            matches++;
                        }
                        block.clear();
                        blockHasMatch = false;
                    }
                    block.add(line);
                    if (line.toLowerCase().contains(lowerKey))
                        blockHasMatch = true;
                }
                if (blockHasMatch) {
                    for (String l : block)
                        results.append(l).append("\n");
                    results.append("--------------------\n");
                    matches++;
                }
            } catch (IOException e) {
            }
        }
        return matches == 0 ? "No matches found." : results.toString();
    }
}