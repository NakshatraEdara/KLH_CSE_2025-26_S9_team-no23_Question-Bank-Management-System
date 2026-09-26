package questionbank.repository;
import java.io.*;
import java.util.*;
import questionbank.model.Question;

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

    /** Reads questions.csv, builds a Question object per line, adds to both structures. */
    public int load(String path) throws IOException {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#") || line.startsWith("id|")) continue;  // skip notes + header
                String[] p = line.split("\\|", 7);           // id|subject|topic|difficulty|marks|tags|text
                if (p.length < 7) { System.out.println("Skipped bad line: " + line); continue; }
                List<String> tags = new ArrayList<>();
                for (String t : p[5].split(";")) if (!t.trim().isEmpty()) tags.add(t.trim());
                Question q = new Question(Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(),
                        p[3].trim(), Integer.parseInt(p[4].trim()), tags, p[6].trim());
                if (add(q)) count++;
                else System.out.println("Duplicate ID skipped while loading: " + q.getId());
            }
        }
        return count;
    }

    /** Writes the ArrayList back to the CSV (so add / update / delete are saved). */
    public void save(String path) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            for (String c : NOTES) pw.println(c);
            pw.println("id|subject|topic|difficulty|marks|tags|text");
            for (Question q : list) pw.println(q.toCsv());
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
