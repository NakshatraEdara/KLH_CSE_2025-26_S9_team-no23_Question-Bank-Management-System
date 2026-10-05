import java.io.*;
import java.util.*;

/**
 * Stores all questions in TWO structures that always hold the SAME Question objects:
 *   ArrayList<Question>          -> ordered storage, display, subject search (traversal)
 *   HashMap<Integer, Question>   -> id -> question, O(1) average lookup / duplicate check
 */
public class QuestionBank {
    private static final String[] NOTES = {
        "# QUESTION BANK DATASET - lines starting with # are notes and are ignored by the program",
        "# Every data row has 7 columns separated by the | symbol:",
        "#   id         = unique question number (whole number, must never repeat)",
        "#   subject    = the course the question belongs to (e.g. Data Structures)",
        "#   topic      = the main topic inside that subject (e.g. Stack)",
        "#   difficulty = Easy, Medium or Hard",
        "#   marks      = marks given for the question (a number: Easy 2, Medium 5, Hard 10)",
        "#   tags       = ALL topics the question covers, separated by ; (used by the bitmask DP)",
        "#   text       = the actual question (must not contain the | symbol)"
    };

    private final ArrayList<Question> list = new ArrayList<>();
    private final HashMap<Integer, Question> map = new HashMap<>();

    // ---------------- DATASET CONNECTION ----------------

    /** Reads all question_*.txt files from a directory, builds Question objects and adds them. */
    public int load(String path) throws IOException {
        int count = 0;
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        java.nio.file.Path dir = java.nio.file.Files.isDirectory(p) ? p : p.getParent();
        if (dir == null || !java.nio.file.Files.isDirectory(dir)) {
            throw new IOException("Invalid questions directory: " + path);
        }
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
            java.util.List<java.nio.file.Path> txtFiles = files
                    .filter(f -> f.getFileName().toString().matches("question_\\d{3}\\.txt"))
                    .sorted()
                    .toList();
            for (java.nio.file.Path file : txtFiles) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(file);
                // Simple parsing based on expected prefixes
                int id = -1; String subject = ""; String topic = ""; String difficulty = ""; int marks = 0; java.util.List<String> tags = new java.util.ArrayList<>();
                StringBuilder textBuilder = new StringBuilder();
                boolean inQuestionPart = false;
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Question ID:")) {
                        id = Integer.parseInt(line.substring("Question ID:".length()).trim());
                    } else if (line.startsWith("Subject:")) {
                        subject = line.substring("Subject:".length()).trim();
                    } else if (line.startsWith("Topic:")) {
                        topic = line.substring("Topic:".length()).trim();
                    } else if (line.startsWith("Difficulty:")) {
                        difficulty = line.substring("Difficulty:".length()).trim();
                    } else if (line.startsWith("Marks:")) {
                        marks = Integer.parseInt(line.substring("Marks:".length()).trim());
                    } else if (line.startsWith("Question:")) {
                        inQuestionPart = true;
                        textBuilder.append(line).append("\n");
                    } else if (inQuestionPart) {
                        textBuilder.append(line).append("\n");
                    }
                }
                if (id == -1) continue; // skip malformed
                Question q = new Question(id, subject, topic, difficulty, marks, tags, textBuilder.toString().trim());
                if (add(q)) count++; // add ensures uniqueness
            }
        }
        return count;
    }

    /** Writes each Question to its own .txt file in the given directory. */
    public void save(String dirPath) throws IOException {
        java.nio.file.Path dir = java.nio.file.Paths.get(dirPath);
        if (!java.nio.file.Files.isDirectory(dir)) {
            throw new IOException("Invalid directory for saving questions: " + dirPath);
        }
        for (Question q : list) {
            String fileName = String.format("question_%03d.txt", q.getId());
            java.nio.file.Path file = dir.resolve(fileName);
            java.util.List<String> lines = java.util.Arrays.asList(
                    "Question ID: " + q.getId(),
                    "Subject: " + q.getSubject(),
                    "Topic: " + q.getTopic(),
                    "Difficulty: " + q.getDifficulty(),
                    "Marks: " + q.getMarks(),
                    "",
                    q.getText()
            );
            java.nio.file.Files.write(file, lines);
        }
    }

    // ---------------- CRUD ----------------

    /** CREATE. Returns false (and stores nothing) if the ID already exists. O(1) average. */
    public boolean add(Question q) {
        if (map.containsKey(q.getId())) return false;   // duplicate ID check
        list.add(q);                                     // append at end of ArrayList
        map.put(q.getId(), q);                           // id -> same object
        return true;
    }

    /** READ by ID. HashMap.get -> O(1) average. */
    public Question getById(int id) { return map.get(id); }

    public boolean contains(int id) { return map.containsKey(id); }

    /** READ by subject. Traverses the ArrayList, equalsIgnoreCase -> O(n). */
    public List<Question> searchBySubject(String subject) {
        List<Question> result = new ArrayList<>();
        for (Question q : list) {
            if (q.getSubject().equalsIgnoreCase(subject)) result.add(q);
        }
        return result;
    }

    /** UPDATE. Finds the object through the HashMap and changes only its text (same object). */
    public boolean updateText(int id, String newText) {
        Question q = map.get(id);          // O(1) average
        if (q == null) return false;
        q.setText(newText);                // ArrayList holds the same object, so it sees the change
        return true;
    }

    /** DELETE. Must remove from BOTH structures so they never disagree. */
    public boolean delete(int id) {
        Question q = map.remove(id);       // O(1) average, also gives us the object
        if (q == null) return false;
        list.remove(q);                    // O(n): search for the object + shift elements left
        return true;
    }

    public List<Question> getAll() { return Collections.unmodifiableList(list); }

    public int size() { return list.size(); }
}
