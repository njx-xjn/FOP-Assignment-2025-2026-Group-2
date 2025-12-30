import java.io.*;
import java.util.*;

public class CSVfile {
    public Map<String, employee> loadEmployee() {
        Map<String, employee> employee = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("employee.csv"))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                // Trim all fields to remove potential whitespace
                String id = d[0].trim();
                String name = d[1].trim();
                String role = d[2].trim();
                String pass = d[3].trim();
                String outletCode = (d.length > 4) ? d[4].trim() : "C60"; // Default to C60 if missing

                if (role.equalsIgnoreCase("Manager"))
                    employee.put(id, new manager(id, name, role, pass, outletCode));
                else
                    employee.put(id, new employee(id, name, role, pass, outletCode));
            }
        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        }
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
            pw.println("EmployeeID,EmployeeName,Role,Password,OutletCode"); // Header
            for (employee e : employee.values()) {
                pw.printf("%s,%s,%s,%s,%s\n", e.getID(), e.getName(), e.getRole(), e.getPassword(), e.getOutlet());
            }
        } catch (IOException e) {
            System.out.println("Error updating employees: " + e.getMessage());
        }
    }

}
