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
            System.out.println("5. Update Question");
            System.out.println("6. Delete Question");
            System.out.println("7. Exit");

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
                    updateQuestion();
                    break;

                case 6:
                    deleteQuestion();
                    break;

                case 7:
                    System.out.println(
                            "Thank you for using the system!"
                    );
                    break;

                default:
                    System.out.println(
                            "Invalid choice! Please try again."
                    );
            }

        } while (choice != 7);

        scanner.close();
    }
}
