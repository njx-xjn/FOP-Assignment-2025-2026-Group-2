public class employee {
    private String id;
    private String name;
    private String password;
    private String role;
    private String outletCode;

    public employee(String id, String name, String role, String password, String outletCode) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.role = role;
        this.outletCode = outletCode;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getPassword() {
        return password;
    }

    public String getOutlet() {
        return outletCode;
    }
}
