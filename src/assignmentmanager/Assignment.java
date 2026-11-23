package assignmentmanager;

/**
 * Model class representing an Assignment.
 */
public class Assignment {
    private int id;
    private String courseCode;
    private String title;
    private String status;   // "Pending" or "Completed"
    private String dueDate;  // e.g. "2026-12-05"

    public Assignment(int id, String courseCode, String title, String status, String dueDate) {
        this.id = id;
        this.courseCode = courseCode != null ? courseCode.trim().toUpperCase() : "";
        this.title = title != null ? title.trim() : "";
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : "Pending";
        this.dueDate = dueDate != null ? dueDate.trim() : "";
    }

    public Assignment(int id, String courseCode, String title) {
        this(id, courseCode, title, "Pending", "");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode != null ? courseCode.trim().toUpperCase() : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim() : "Pending";
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate != null ? dueDate.trim() : "";
    }

    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "#" + id + " [" + courseCode + "] " + title + " (" + status + ")";
    }
}
