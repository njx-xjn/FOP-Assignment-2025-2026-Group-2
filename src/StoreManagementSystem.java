import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StoreManagementSystem {
    private employee loggedInEmployee=null;
    private CSVfile fh=new CSVfile();
    private Map<String,employee> employee;
    private List<attendance> attendanceLog=new ArrayList<>();
    private Map<String, String> outlet;
    private Scanner sc=new Scanner(System.in);
    
    public void start(){
        while (true){
            employee=fh.loadEmployee();
            outlet=fh.loadOutlets();
            if (loggedInEmployee==null){
                loginMenu();
            }
            else
                mainMenu();
        }
    }
    
    private void loginMenu(){
        //String id,password,name,role,outletCode;
        //boolean getLogin=false;
        System.out.println("== Employee Login ==");
        System.out.print("Enter User ID: ");
        String id=sc.nextLine();
        System.out.print("Enter Password: ");
        String password=sc.nextLine();
        if(employee.containsKey(id)&&employee.get(id).getPassword().equals(password)){
            loggedInEmployee=employee.get(id);
            System.out.println("Login Successful!");
            System.out.printf("Welcome, %s (%s)\n",loggedInEmployee.getName(),loggedInEmployee.getOutlet());
        }
        else
            System.out.println("Login Failed: Invalid User ID or Password.");
        
        /*int i;
        for (i = 0; i < employee.length; i++) {
            if (id.equals(employee[i][0]) && password.equals(employee[i][3])){
                getLogin=true;
                break;
             }            
        }
        name=employee[i][1];
        role=employee[i][2];
        outletCode=id.substring(0,3);
        
        if(getLogin){
            if (role.equals("Manager"))
                loggedInEmployee=new manager(id,password,name,role);
            else
                loggedInEmployee=new employee(id,password,name,role);
            System.out.println("Login Successful!");
            System.out.printf("Welcome, %s (%s)\n",name,outletCode);
        }
        else{
            System.out.println("Login Failed: Invalid User ID or Password.");
            loggedInEmployee=null;
        }*/
    }
    
    private void mainMenu(){
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. Clock In");
        System.out.println("2. Clock Out");
        System.out.println("3. View Stock");
        System.out.println("4. Record Sale");
        System.out.println("5. Search Information");
        System.out.println("6. Logout");
        if (loggedInEmployee instanceof manager) System.out.println("7. Register Employee");
        System.out.print("Choose option: ");
        int choice = sc.nextInt();
        sc.nextLine();
        switch (choice) {
            case 1 : clock(1); break;
            case 2 : clock(2); break;
            case 3 : viewStock(); break;
            case 4 : recordSale(); break;
            case 5 : searchInfo(); break;
            case 6 : logOut(); break;
            case 7 : if (loggedInEmployee instanceof manager) {register(); break;}
            default : System.out.println("Invalid choice.");
        }
    }
    
    private void clock(int n){
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        if(n==1){
            attendanceLog.add(new attendance(loggedInEmployee.getID(),date,time));
            System.out.println("=== Attendance Clock In ===");
            System.out.println("Employee ID: "+loggedInEmployee.getID());
            System.out.println("Name: "+loggedInEmployee.getName());
            System.out.printf("Outlet: %s (%s)\n\n",loggedInEmployee.getOutlet(),outlet.get(loggedInEmployee.getOutlet()));
            System.out.println("Clock In Successful!");
            System.out.println("Date: "+date);
            System.out.println("Time: "+time);
        }
        else{
            for (attendance a:attendanceLog){
                if (a.getID().equals(loggedInEmployee.getID()) && a.getClockOut()==null){
                    a.clockOut(time);
                    System.out.println("=== Attendance Clock Out ===");
                    System.out.println("Employee ID: "+loggedInEmployee.getID());
                    System.out.println("Name: "+loggedInEmployee.getName());
                    System.out.printf("Outlet: %s (%s)\n\n",loggedInEmployee.getOutlet(),outlet.get(loggedInEmployee.getOutlet()));
                    System.out.println("Clock Out Successful!");
                    System.out.println("Date: "+date);
                    System.out.println("Time: "+time);
                    System.out.println("Total Hours Worked: "+a.getTotalHour()+" hours");
                    break;
                }
            }
        }
    }
    
    private void viewStock(){

    }
    
    private void recordSale(){
        
    }
    
    private void searchInfo(){
        
    }
    
    private void logOut(){
        System.out.println("\nLogging out "+loggedInEmployee.getName()+"\n");
        loggedInEmployee=null;
    }
    
    private void register(){
        System.out.println("\n=== Register New Employee ===");
        System.out.print("Enter Employee Name: ");
        String name=sc.nextLine();
        System.out.print("Enter Employee ID: ");
        String id=sc.nextLine();
        System.out.print("Set password: ");
        String password=sc.nextLine();
        System.out.print("Set role: ");
        String role=sc.nextLine();
        employee.put(id,new employee(id,name,role,password));
        fh.uploadEmployeeCSV(employee);
    }
}
