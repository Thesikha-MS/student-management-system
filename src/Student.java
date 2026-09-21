import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple POJO representing a Student record.
 */
public class Student {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

    private final int id;
    private String name;
    private String email;
    private String course;
    private double marks;

    public Student(String name, String email, String course, double marks) {
        this.id = SEQUENCE.incrementAndGet();
        this.name = name;
        this.email = email;
        this.course = course;
        this.marks = marks;
    }

    // Constructor used when rebuilding from storage with a known id (e.g. on update)
    public Student(int id, String name, String email, String course, double marks) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.course = course;
        this.marks = marks;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCourse() { return course; }
    public double getMarks() { return marks; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setCourse(String course) { this.course = course; }
    public void setMarks(double marks) { this.marks = marks; }

    /** Serializes this student to a single JSON object string. */
    public String toJson() {
        return "{"
                + "\"id\":" + id + ","
                + "\"name\":" + Json.quote(name) + ","
                + "\"email\":" + Json.quote(email) + ","
                + "\"course\":" + Json.quote(course) + ","
                + "\"marks\":" + marks
                + "}";
    }
}
