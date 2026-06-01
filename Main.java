class Employee {
protected String name;
private double salary;
Employee(String name, double salary) {
this.name = name;
this.salary = salary;
}
public double income() {
return salary;
}
public String role() {
return "Employee";
}
public static String policy() {
return "standard";
}
}
class Manager extends Employee {
private double bonus;
Manager(String name, double salary, double bonus) {
super(name, salary);
this.bonus = bonus;
}
@Override
public double income() {
return super.income() + bonus;
}

@Override
public String role() {
return "Manager";
}
public static String policy() {
return "management";
}
}
public class Main {
public static void main(String[] args) {
Employee e = new Manager("Lan", 1000, 300);
System.out.println(e.role());
System.out.println(e.income());
System.out.println(e.policy());
System.out.println(((Manager) e).policy());
}
}