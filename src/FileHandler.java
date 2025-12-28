// old one, use array
import java.io.*;
import java.util.*;
public class FileHandler {
    private String [][] employee;
    private String [][] outlet;
    private String [][] model;
    private String [][] attendance;
    
    public String[][] loadEmployee(){
        int row = 0;
        int col = 0;
        String fileName="employee.csv";
        try (BufferedReader in=new BufferedReader(new FileReader(fileName))){
            String line;
            while ((line=in.readLine())!=null){
                row++;
                if (row == 1) {
                    col = line.split(",").length;
                }
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File employee.csv not found");
        }
        catch (IOException e){
            System.out.println("Problem with file employee.csv input");
        }
        employee = new String[row][col];
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int rowIndex = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int colIndex = 0; colIndex < col; colIndex++) {
                    employee[rowIndex][colIndex] = values[colIndex];
                }
                rowIndex++;
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        return employee;
    }
    
    public String[][] loadOutlet(){
        int row = 0;
        int col = 0;
        String fileName="outlet.csv";
        try (BufferedReader in=new BufferedReader(new FileReader(fileName))){
            String line;
            while ((line=in.readLine())!=null){
                row++;
                if (row == 1) {
                    col = line.split(",").length;
                }
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        outlet = new String[row][col];
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int rowIndex = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int colIndex = 0; colIndex < col; colIndex++) {
                    outlet[rowIndex][colIndex] = values[colIndex];
                }
                rowIndex++;
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        return outlet;
    }
    
    public String[][] loadModel(){
        int row = 0;
        int col = 0;
        String fileName="model.csv";
        try (BufferedReader in=new BufferedReader(new FileReader(fileName))){
            String line;
            while ((line=in.readLine())!=null){
                row++;
                if (row == 1) {
                    col = line.split(",").length;
                }
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        model = new String[row][col];
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int rowIndex = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int colIndex = 0; colIndex < col; colIndex++) {
                    model[rowIndex][colIndex] = values[colIndex];
                }
                rowIndex++;
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        return model;
    }
    
    public String[][] loadAttendance(){
        int row = 0;
        int col = 0;
        String fileName="attendance.csv";
        try (BufferedReader in=new BufferedReader(new FileReader(fileName))){
            String line;
            while ((line=in.readLine())!=null){
                row++;
                if (row == 1) {
                    col = line.split(",").length;
                }
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        attendance = new String[row][col];
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int rowIndex = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int colIndex = 0; colIndex < col; colIndex++) {
                    attendance[rowIndex][colIndex] = values[colIndex];
                }
                rowIndex++;
            }
        }
        catch (FileNotFoundException e){
            System.out.println("File "+fileName+" not found");
        }
        catch (IOException e){
            System.out.println("Problem with file "+fileName+" input");
        }
        return attendance;
    }
    
    public String[][] getEmployee(){
        return employee;
    }
    
    public String[][] getOutlet(){
        return outlet;
    }
    
    public String[][] getModel(){
        return model;
    }
    
    public String[][] getAttendance(){
        return attendance;
    }
    
    public void uploadEmployeeCSV(String id,String name,String role,String password){
        String newRow =id+","+name+","+role+","+password;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("employee.csv", true))) {
            bw.newLine();
            bw.write(newRow);
            System.out.println("Employee successfully registered");
        } catch (IOException e) {
            System.out.println("Problem with file employee.csv output");
        }
    }
    
    public void uploadModelCSV(String [][] model){
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("employee.csv"))) {
            bw.write("Model,Price,C60,C61,C62,C63,C64,C65,C66,C67,C68,C69");
            for(int i=0;i<model.length;i++){
                bw.newLine();
                bw.write(model[i][0]);
                for (int j=1;j<model[i].length;j++){
                    bw.write(model[i][j]+",");
                }
            }
        } catch (IOException e) {
            System.out.println("Problem with file employee.csv output");
        }
    }
    
    public void uploadAttendance(String date,String id,String clockIn,String clockOut){
        if (clockOut.equals(null)){
            String row =date+","+id+","+clockIn+","+clockOut;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("attendance.csv", true))) {
                bw.newLine();
                bw.write(row);
            } 
            catch (IOException e) {
                System.out.println("Problem with file attendance.csv output");
            }
        }
        
            
    }
    public void saveSales(){
        
    }
    
    public void writeReceipt(){
        
    }
}
