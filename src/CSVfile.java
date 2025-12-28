import java.io.*;
import java.util.*;

public class CSVfile{
    public Map<String, employee> loadEmployee() {
        Map<String, employee> employee = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("employee.csv"))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d[2].equals("Manager")) 
                    employee.put(d[0], new manager(d[0], d[1], d[2], d[3]));
                else 
                    employee.put(d[0], new employee(d[0], d[1], d[2], d[3]));
            }
        } catch (IOException e) { System.out.println("File Error: " + e.getMessage()); }
        return employee;
    }
    
    public Map<String, String> loadOutlets() {
        Map<String, String> outlet = new LinkedHashMap<>(); // LinkedHashMap preserves order
        try (BufferedReader br = new BufferedReader(new FileReader("outlet.csv"))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 2) {
                    outlet.put(data[0].trim(), data[1].trim()); // OutletCode, OutletName 
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading outlets: " + e.getMessage());
        }
        return outlet;
    }
    
    public void uploadEmployeeCSV(Map<String, employee> employee) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("employee.csv"))) {
            pw.println("EmployeeID,EmployeeName,Role,Password"); // Header
            for (employee e : employee.values()) {
                pw.printf("%s,%s,%s,%s\n", e.getID(), e.getName(), e.getRole(), e.getPassword());
            }
        } catch (IOException e) {
            System.out.println("Error updating employees: " + e.getMessage());
        }
    }
    
    
}
