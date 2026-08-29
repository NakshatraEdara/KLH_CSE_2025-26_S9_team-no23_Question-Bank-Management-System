import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

class Question {
    int id;
    String subject;
    String topic;
    String difficulty;
    String questionText;

    Question(int id, String subject, String topic,
             String difficulty, String questionText) {
        this.id = id;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionText = questionText;
    }

    public String toString() {
        return "\nQuestion ID: " + id +
                "\nSubject: " + subject +
                "\nTopic: " + topic +
                "\nDifficulty: " + difficulty +
                "\nQuestion: " + questionText;
    }
}

public class Main {

    static ArrayList<Question> questions = new ArrayList<>();
    static HashMap<Integer, Question> questionMap = new HashMap<>();
    static Scanner scanner = new Scanner(System.in);

    // =========================================================
    // 1. ADD QUESTION
    // =========================================================
    static void addQuestion() {

        System.out.print("Enter Question ID: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        if (questionMap.containsKey(id)) {
            System.out.println("Question ID already exists!");
            return;
        }

        System.out.print("Enter Subject: ");
        String subject = scanner.nextLine();

        System.out.print("Enter Topic: ");
        String topic = scanner.nextLine();

        System.out.print("Enter Difficulty (Easy/Medium/Hard): ");
        String difficulty = scanner.nextLine();

        System.out.print("Enter Question: ");
        String questionText = scanner.nextLine();

        Question question = new Question(
                id, subject, topic, difficulty, questionText
        );

        questions.add(question);
        questionMap.put(id, question);

        System.out.println("Question added successfully!");
    }

    // =========================================================
    // 2. DISPLAY ALL QUESTIONS
    // =========================================================
    static void displayQuestions() {

        if (questions.isEmpty()) {
            System.out.println("No questions available.");
            return;
        }

        System.out.println("\n===== ALL QUESTIONS =====");

        for (Question question : questions) {
            System.out.println(question);
            System.out.println("-------------------------");
        }
    }

    // =========================================================
    // 3. SEARCH QUESTION BY ID
    // =========================================================
    static void searchQuestion() {

        System.out.print("Enter Question ID: ");
        int id = scanner.nextInt();

        Question question = questionMap.get(id);

        if (question != null) {
            System.out.println("\nQuestion Found:");
            System.out.println(question);
        } else {
            System.out.println("Question not found!");
        }
    }

    // =========================================================
    // 4. SEARCH QUESTIONS BY SUBJECT
    // =========================================================
    static void searchBySubject() {

        scanner.nextLine();

        System.out.print("Enter Subject: ");
        String subject = scanner.nextLine();

        boolean found = false;

        for (Question question : questions) {

            if (question.subject.equalsIgnoreCase(subject)) {
                System.out.println(question);
                System.out.println("-------------------------");
                found = true;
            }
        }

        if (!found) {
            System.out.println("No questions found for this subject.");
        }
    }

    // =========================================================
    // KMP ALGORITHM
    // =========================================================

    // Create LPS array
    static int[] buildLPS(String pattern) {

        int[] lps = new int[pattern.length()];

        int length = 0;
        int i = 1;

        while (i < pattern.length()) {

            if (Character.toLowerCase(pattern.charAt(i)) ==
                    Character.toLowerCase(pattern.charAt(length))) {

                length++;
                lps[i] = length;
                i++;

            } else {

                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }

    // KMP search
    // Returns true if pattern is found in text
    static boolean kmpSearch(String text, String pattern) {

        if (pattern.isEmpty()) {
            return true;
        }

        int[] lps = buildLPS(pattern);

        int i = 0; // text index
        int j = 0; // pattern index

        while (i < text.length()) {

            if (Character.toLowerCase(text.charAt(i)) ==
                    Character.toLowerCase(pattern.charAt(j))) {

                i++;
                j++;

                // Entire pattern matched
                if (j == pattern.length()) {
                    return true;
                }

            } else {

                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return false;
    }

    // =========================================================
    // 5. SEARCH QUESTIONS BY PHRASE USING KMP
    // =========================================================
    static void searchByPhraseKMP() {

        scanner.nextLine();

        System.out.print("Enter phrase to search: ");
        String phrase = scanner.nextLine();

        if (phrase.isEmpty()) {
            System.out.println("Search phrase cannot be empty!");
            return;
        }

        boolean found = false;

        System.out.println("\n===== KMP SEARCH RESULTS =====");

        for (Question question : questions) {

            // Search inside question text
            if (kmpSearch(question.questionText, phrase)) {

                System.out.println(question);
                System.out.println("-------------------------");

                found = true;
            }
        }

        if (!found) {
            System.out.println("No questions found containing: " + phrase);
        }
    }

    // =========================================================
    // 6. UPDATE QUESTION
    // =========================================================
    static void updateQuestion() {

        System.out.print("Enter Question ID: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        Question question = questionMap.get(id);

        if (question != null) {

            System.out.print("Enter New Question: ");
            question.questionText = scanner.nextLine();

            System.out.println("Question updated successfully!");

        } else {
            System.out.println("Question not found!");
        }
    }

    // =========================================================
    // 7. DELETE QUESTION
    // =========================================================
    static void deleteQuestion() {

        System.out.print("Enter Question ID: ");
        int id = scanner.nextInt();

        Question question = questionMap.get(id);

        if (question != null) {

            questions.remove(question);
            questionMap.remove(id);

            System.out.println("Question deleted successfully!");

        } else {
            System.out.println("Question not found!");
        }
    }

    // =========================================================
    // MAIN METHOD
    // =========================================================
    public static void main(String[] args) {

        int choice;

        do {

            System.out.println("\n=================================");
            System.out.println(" QUESTION BANK MANAGEMENT SYSTEM");
            System.out.println("=================================");

            System.out.println("1. Add Question");
            System.out.println("2. Display All Questions");
            System.out.println("3. Search Question by ID");
            System.out.println("4. Search Questions by Subject");
            System.out.println("5. Search Questions by Phrase (KMP)");
            System.out.println("6. Update Question");
            System.out.println("7. Delete Question");
            System.out.println("8. Exit");

            System.out.print("Enter your choice: ");

            choice = scanner.nextInt();

            switch (choice) {

                case 1:
                    addQuestion();
                    break;

                case 2:
                    displayQuestions();
                    break;

                case 3:
                    searchQuestion();
                    break;

                case 4:
                    searchBySubject();
                    break;

                case 5:
                    searchByPhraseKMP();
                    break;

                case 6:
                    updateQuestion();
                    break;

                case 7:
                    deleteQuestion();
                    break;

                case 8:
                    System.out.println(
                            "Thank you for using the system!"
                    );
                    break;

                default:
                    System.out.println(
                            "Invalid choice! Please try again."
                    );
            }

        } while (choice != 8);

        scanner.close();
    }
}

