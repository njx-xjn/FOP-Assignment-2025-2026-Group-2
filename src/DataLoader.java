import java.io.*;
import java.util.*;

// THE DATABASE ENGINE
// This class handles ALL reading and writing to the text/CSV files.
// It acts as the "Server" or "Backend" for your application.
public class DataLoader {
    
    // File names constants. We use constants so if we rename a file, 
    // we only have to change it here, not in 50 other places.
    private static final String EMPLOYEE_FILE = "employee.csv";
    private static final String OUTLET_FILE = "outlet.csv";
    private static final String MODEL_FILE = "model.csv";
    private static final String ATTENDANCE_FILE = "attendance.csv";
    
    // --- LOAD EMPLOYEES ---
    // ** CONNECTION TO GUI.JAVA **
    // Called in GUI constructor to load the login credentials.
    public Map<String, employee> loadEmployee() {
        // TreeMap sorts keys automatically (so Employee IDs are in order: 001, 002...)
        Map<String, employee> employee = new TreeMap<>();
        
        // Try-with-resources: Automatically closes the file even if an error crashes the app
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {
            br.readLine(); // READ HEADER: We ignore the first line (labels)
            
            String line;
            // Loop through the rest of the file line by line
            while ((line = br.readLine()) != null) {
                // Split the comma-separated line into an array
                String[] d = line.split(",");
                
                // DATA CLEANING: .trim() removes accidental spaces like " Admin " -> "Admin"
                String id = d[0].trim();
                String name = d[1].trim();
                String role = d[2].trim();
                String pass = d[3].trim();
                
                // SAFETY CHECK: Some older CSV rows might not have an Outlet Code.
                // If it's missing, we default to "C60" so the app doesn't crash.
                String outletCode = (d.length > 4) ? d[4].trim() : "C60"; 

                // POLYMORPHISM: Create a Manager object or a regular Employee object
                // based on the role text.
                if (role.equalsIgnoreCase("Manager"))
                    employee.put(id, new manager(id, name, role, pass, outletCode));
                else
                    employee.put(id, new employee(id, name, role, pass, outletCode));
            }
        } catch (IOException e) {
            // If file is missing or locked, print error to console
            System.out.println("File Error: " + e.getMessage());
        }
        return employee;
    }

    // --- LOAD OUTLETS ---
    // ** USED BY STOCKINOUTTAB & SEARCHPANEL **
    // Loads the list of stores (e.g., C60 -> Kuala Lumpur)
    public Map<String, String> loadOutlets() {
        // LinkedHashMap preserves the order from the text file (Top to Bottom)
        Map<String, String> outlet = new LinkedHashMap<>(); 
        try (BufferedReader br = new BufferedReader(new FileReader(OUTLET_FILE))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                // Ensure the line actually has data before trying to read it
                if (data.length >= 2) {
                    outlet.put(data[0].trim(), data[1].trim()); // Key=Code (C60), Value=Name (KL)
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading outlets: " + e.getMessage());
        }
        return outlet;
    }

    // --- SAVE EMPLOYEES ---
    // ** USED BY REGISTERPANEL **
    // Overwrites the entire CSV file with the new list of employees.
    public void uploadEmployeeCSV(Map<String, employee> employee) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EMPLOYEE_FILE))) {
            // Step 1: Write the Header row first
            pw.println("EmployeeID,EmployeeName,Role,Password,OutletCode"); 
            
            // Step 2: Loop through every employee in memory and write them to file
            for (employee e : employee.values()) {
                // Formatting: %s inserts the string variables into the CSV format
                pw.printf("%s,%s,%s,%s,%s\n", e.getID(), e.getName(), e.getRole(), e.getPassword(), e.getOutlet());
            }
        } catch (IOException e) {
            System.out.println("Error updating employees: " + e.getMessage());
        }
    }
    
    // --- LOAD MODELS (STOCK) ---
    // ** USED BY STOCKCOUNTTAB, SALESPANEL, ETC **
    // This is complex because the CSV has dynamic columns (one column per outlet).
    // File structure: Model,Price,Outlet1_Qty,Outlet2_Qty...
    public Map<String, Model> loadModels() {
        Map<String, Model> models = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(MODEL_FILE))) {
            String header = br.readLine();
            // We need the header to know which column belongs to which Outlet (e.g. C60, C61)
            String[] headers = header.split(","); 

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                
                // Basic Validation: Must have at least Name and Price
                if (data.length > 2) {
                    String name = data[0];
                    double price = Double.parseDouble(data[1]); // Convert text "10.50" to number 10.50
                    
                    // Create the base Model object
                    Model m = new Model(name, price);

                    // DYNAMIC PARSING LOOP
                    // Start at index 2 because 0 is Name and 1 is Price.
                    // Everything after that is stock quantity for different stores.
                    for (int i = 2; i < data.length; i++) {
                        if (i < headers.length) {
                            // headers[i] is the Outlet Name (e.g., "C60")
                            // data[i] is the Quantity (e.g., "5")
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

    // --- SAVE MODELS ---
    // ** USED BY SALESPANEL & STOCKINOUTTAB **
    // Saves changes to stock quantity back to the file.
    public void saveModels(Map<String, Model> models, List<String> outletCodes) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MODEL_FILE))) {
            // Step 1: Rebuild the Header.
            // We start with "Model,Price" and then loop to add every Outlet Code.
            StringBuilder header = new StringBuilder("Model,Price");
            for (String code : outletCodes) {
                header.append(",").append(code);
            }
            pw.println(header);

            // Step 2: Write the Data Rows.
            for (Model m : models.values()) {
                StringBuilder row = new StringBuilder();
                // Append static data
                row.append(m.getModelName()).append(",").append(m.getPrice());
                
                // Append dynamic stock data in the exact same order as the header
                for (String code : outletCodes) {
                    row.append(",").append(m.getStock(code));
                }
                pw.println(row);
            }
        } catch (IOException e) {
            System.out.println("Error saving models: " + e.getMessage());
        }
    }
    
    // --- LOAD TRANSACTIONS (PARSER) ---
    // ** USED BY ANALYTICSPANEL & HISTORYPANEL **
    // This reads the receipt text files and converts them back into Java Objects.
    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        File dir = new File("SalesReceipt");

        // If the folder doesn't exist, there are no sales to load.
        if (!dir.exists() || !dir.isDirectory()) {
            return transactions;
        }

        // Get a list of all files ending in ".txt" inside that folder
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null)
            return transactions;

        // Loop through every single receipt file found
        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                // Buffer variables to hold data as we read line-by-line
                String date = "", time = "", cust = "", model = "", empId = "N/A";
                double amt = 0;
                int qty = 0;

                while ((line = br.readLine()) != null) {
                    // PARSING LOGIC: We look for "Keys" in the text file.
                    // If line is "Date: 2023-10-25", we split by ": " and take the second part.
                    if (line.contains("Date: "))
                        date = line.split(": ")[1].trim();
                    else if (line.contains("Time: "))
                        time = line.split(": ")[1].trim();
                    
                    // Capture Employee ID so we can filter by staff later
                    else if (line.contains("Employee: "))
                        empId = line.split(": ")[1].trim();

                    else if (line.contains("Customer Name: "))
                        cust = line.split(": ")[1].trim();
                    else if (line.contains("Model: "))
                        model = line.split(": ")[1].trim();
                    else if (line.contains("Quantity: "))
                        qty = Integer.parseInt(line.split(": ")[1].trim()); // Convert text "5" to int 5
                    else if (line.contains("Subtotal: RM")) {
                        // Extract money amount (removing the "RM" part)
                        amt = Double.parseDouble(line.split("RM")[1].trim());
                    }

                    // TRIGGER: End of an Item Block
                    // The receipt uses a long dashed line to separate items or end the receipt.
                    // We check length > 40 to distinguish from shorter separators.
                    else if (line.startsWith("----------------") && line.length() > 40) {
                        // If we successfully collected a Model name, this block is valid.
                        if (!model.isEmpty()) {
                            // Create the Transaction object and save it to our list
                            transactions.add(new Transaction("",
                                    "SALE", date, time, empId, "C60", model, qty, amt, cust));
                        }
                        // Reset item-specific fields for the next item in the loop
                        // (Date/Time/Cust usually stay same for the whole receipt)
                        model = "";
                        amt = 0;
                        qty = 0;
                        cust = "";
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing receipt " + f.getName() + ": " + e.getMessage());
            }
        }
        return transactions;
    }

    // --- ATTENDANCE: CLOCK IN ---
    // ** USED BY ATTENDANCETAB **
    public String clockIn(String empId) {
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        
        // CHECK DUPLICATES: Read file first to see if they already clocked in.
        List<String[]> lines = readCSV(ATTENDANCE_FILE);
        for (String[] row : lines) {
            // Logic: ID matches AND Date matches AND ClockOut column is empty/missing
            if (row.length > 0 && row[0].equals(empId) && row[1].equals(date) && (row.length < 4 || row[3].isEmpty())) {
                return "Already Clocked In for today!";
            }
        }
        
        // APPEND: Open file in "Append Mode" (true) to add to the end
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ATTENDANCE_FILE, true))) {
            // Write: ID,Date,Time,, (Empty comma at end implies no clock out yet)
            bw.write(empId + "," + date + "," + time + ",,"); 
            bw.newLine();
            return "Clock In Successful!\nDate: " + date + "\nTime: " + time;
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    // --- ATTENDANCE: CLOCK OUT ---
    // ** USED BY ATTENDANCETAB **
    // This is harder because we can't just "edit" a text file line.
    // We have to: Read ALL lines -> Modify the specific line -> Rewrite the WHOLE file.
    public String clockOut(String empId) {
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        
        List<String[]> lines = readCSV(ATTENDANCE_FILE);
        boolean found = false;
        String result = "";
        
        // List to hold the new file content
        List<String> newLines = new ArrayList<>();
        newLines.add("EmployeeID,Date,ClockInTime,ClockOutTime,TotalHours"); // Re-add Header

        for (int i = 1; i < lines.size(); i++) {
            String[] row = lines.get(i);
            
            // SEARCH: Look for the open session for this user today
            if (!found && row.length > 0 && row[0].equals(empId) && row[1].equals(date)
                    && (row.length < 4 || row[3].isEmpty())) {
                found = true;
                try {
                    // MATH: Calculate hours worked
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                    java.time.LocalTime inTime = java.time.LocalTime.parse(row[2], fmt);
                    java.time.LocalTime outTime = java.time.LocalTime.parse(time, fmt);
                    
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(inTime, outTime);
                    String totalHours = String.format("%.1f hours", minutes / 60.0);
                    
                    // UPDATE: Reconstruct the line with ClockOut Time and Total Hours
                    newLines.add(row[0] + "," + row[1] + "," + row[2] + "," + time + "," + totalHours);
                    
                    result = "Clock Out Successful!\nDate: " + date + "\nTime: " + time + "\nTotal Hours: " + totalHours;
                } catch (Exception e) {
                    // If date math fails, keep line as is to prevent data loss
                    newLines.add(String.join(",", row));
                    result = "Error: " + e.getMessage();
                }
            } else {
                // KEEP: If this isn't the line we want to edit, just add it back exactly as it was
                newLines.add(String.join(",", row)); 
            }
        }
        
        if (!found)
            return "Error: You have not clocked in today!";
            
        // REWRITE: Save the modified list back to the file (overwrite mode)
        try (PrintWriter pw = new PrintWriter(new FileWriter(ATTENDANCE_FILE))) {
            for (String line : newLines)
                pw.println(line);
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
        return result;
    }

    // Helper: Reads CSV into a List of String arrays
    private List<String[]> readCSV(String file) {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null)
                lines.add(line.split(","));
        } catch (IOException e) {
            // Silent fail is okay here, outer methods handle logic
        }
        return lines;
    }

    // --- LOGGING ---
    // Appends text to daily log files (Used by StockInOutTab)
    public void appendReceipt(String content) {
        String directoryName = "StockReceipt";
        File directory = new File(directoryName);
        if (!directory.exists())
            directory.mkdir(); // Auto-create folder if it's missing
            
        String date = java.time.LocalDate.now().toString();
        String filename = directoryName + File.separator + "receipts_" + date + ".txt";
        
        // 'true' means append mode (don't delete existing content)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true))) {
            bw.write(content);
            bw.newLine();
            bw.write("--------------------------------------------------"); // Visual Separator
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving receipt: " + e.getMessage());
        }
    }

    // Appends text to sales logs (Used by SalesPanel)
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

    // --- SEARCH ---
    // ** USED BY SEARCHPANEL **
    // Scans all text files for a specific keyword (like a Customer Name or ID)
    public String searchSalesReceipts(String keyword) {
        File dir = new File("SalesReceipt");
        if (!dir.exists() || !dir.isDirectory())
            return "No records found.";
            
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0)
            return "No records found.";

        StringBuilder results = new StringBuilder();
        int matches = 0;
        String lowerKey = keyword.toLowerCase(); // Case-insensitive search

        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                // Buffer: We store lines of a single receipt here temporarily.
                List<String> block = new ArrayList<>();
                boolean blockHasMatch = false;

                // Read file line by line
                while ((line = br.readLine()) != null) {
                    // Logic: Receipts are separated by "===". 
                    // When we hit a separator, we check if the previous block had the keyword.
                    if (line.startsWith("===") && !block.isEmpty()) {
                        if (blockHasMatch) {
                            // If yes, add the whole block to final results
                            for (String l : block)
                                results.append(l).append("\n");
                            results.append("--------------------\n");
                            matches++;
                        }
                        // Reset for next receipt
                        block.clear();
                        blockHasMatch = false;
                    }
                    
                    // Add current line to buffer
                    block.add(line);
                    
                    // Check if this specific line contains the keyword
                    if (line.toLowerCase().contains(lowerKey))
                        blockHasMatch = true;
                }
                
                // FINAL CHECK: The loop ends before processing the very last block.
                // We must check the buffer one last time.
                if (blockHasMatch) {
                    for (String l : block)
                        results.append(l).append("\n");
                    results.append("--------------------\n");
                    matches++;
                }
            } catch (IOException e) {
                // Ignore read errors for individual files
            }
        }
        return matches == 0 ? "No matches found." : results.toString();
    }
}