package questionbank.model;
import java.util.List;

/**
 * One question in the bank. Plain class: private attributes, a constructor,
 * getters, and a setter only for the text (the only field Update changes).
 */
public class Question {
    private final int id;
    private final String subject;
    private final String topic;
    private final String difficulty;   // Easy / Medium / Hard
    private final int marks;
    private final List<String> tags;   // all topics this question covers (used by DP module)
    private String text;

    public Question(int id, String subject, String topic, String difficulty,
                    int marks, List<String> tags, String text) {
        this.id = id;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.marks = marks;
        this.tags = tags;
        this.text = text;
    }

    public int getId()            { return id; }
    public String getSubject()    { return subject; }
    public String getTopic()      { return topic; }
    public String getDifficulty() { return difficulty; }
    public int getMarks()         { return marks; }
    public List<String> getTags() { return tags; }
    public String getText()       { return text; }

    public void setText(String text) { this.text = text; }

    /** One line in the same format as questions.csv. */
    public String toCsv() {
        return id + "|" + subject + "|" + topic + "|" + difficulty + "|" + marks
                + "|" + String.join(";", tags) + "|" + text;
    }

    /** Short one-line view used when listing questions. */
    @Override
    public String toString() {
        return String.format("%3d | %-17s | %-18s | %-6s | %2d | %s",
                id, subject, topic, difficulty, marks, text);
    }
}
