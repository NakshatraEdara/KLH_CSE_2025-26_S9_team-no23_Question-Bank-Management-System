package questionbank.app;

import java.io.*;
import java.util.*;

import questionbank.model.Question;
import questionbank.repository.QuestionBank;
import questionbank.algorithms.string.StringAlgorithms;
import questionbank.algorithms.string.SuffixTools;
import questionbank.exam.ExamBuilder;

public class Main {

    static final String DEFAULT_QUESTIONS = "data/questions.csv";

    static final Scanner sc = new Scanner(System.in);
    static final QuestionBank bank = new QuestionBank();
    static String questionsFile;


    public static void main(String[] args) {

        questionsFile = args.length > 0 ? args[0] : DEFAULT_QUESTIONS;

        try {
            int n = bank.load(questionsFile);

            System.out.println("Loaded " + n + " questions from " + questionsFile);

        } catch (IOException | NumberFormatException e) {

            System.out.println(
                    "Could not read dataset '" + questionsFile + "': "
                            + e.getMessage()
            );

            System.out.println(
                    "Run the program from the QuestionBankSystem folder, "
                            + "or pass the file path as an argument."
            );

            return;
        }


        while (true) {

            printMenu();

            int choice = readInt("Enter choice: ");

            System.out.println();


            switch (choice) {

                case 1:
                    addQuestion();
                    break;

                case 2:
                    displayAll();
                    break;

                case 3:
                    searchById();
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
                    keywordSearch();
                    break;

                case 8:
                    repeatedPhrases();
                    break;

                case 9:
                    benchmark();
                    break;

                case 10:
                    buildExam();
                    break;

                case 11:
                    saveNow();
                    break;

                case 0:
                    saveNow();
                    System.out.println("Goodbye.");
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }


    static void printMenu() {

        System.out.println();

        System.out.println(
                "=============================================================="
        );

        System.out.println(
                "             QUESTION BANK MANAGEMENT SYSTEM"
        );

        System.out.println(
                "                  (" + bank.size() + " questions)"
        );

        System.out.println(
                "=============================================================="
        );


        System.out.println();

        System.out.println(
                "                        CRUD OPERATIONS"
        );

        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println("  1. Add question");
        System.out.println("  2. Display all questions");
        System.out.println("  3. Search question by ID");
        System.out.println("  4. Search questions by subject");
        System.out.println("  5. Update question");
        System.out.println("  6. Delete question");


        System.out.println();

        System.out.println(
                "                     STRING SEARCHING"
        );

        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println(
                "  7. Keyword search in question text"
        );

        System.out.println(
                "  8. Find repeated phrases"
        );

        System.out.println(
                "  9. Benchmark string matching algorithms"
        );


        System.out.println();

        System.out.println(
                "                   EXAM SET GENERATION"
        );

        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println(
                " 10. Build exam sets"
        );


        System.out.println();

        System.out.println(
                "                      FILE OPERATIONS"
        );

        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println(" 11. Save now");
        System.out.println("  0. Save & Exit");


        System.out.println(
                "=============================================================="
        );
    }


    static void addQuestion() {

        printSection("ADD QUESTION");


        int id = readInt("Question ID: ");


        if (bank.contains(id)) {

            System.out.println();

            System.out.println(
                    "ID " + id + " already exists."
            );

            System.out.println(
                    "Nothing was added."
            );

            return;
        }


        String subject = readLine("Subject: ");

        String topic = readLine("Topic: ");


        String difficulty = null;


        while (difficulty == null) {

            difficulty = normalizeDifficulty(
                    readLine("Difficulty (Easy / Medium / Hard): ")
            );
        }


        int marks = readInt("Marks: ");


        String tagLine = readLine(
                "Tags (use ; between tags, or press Enter to use the topic): "
        );


        List<String> tags = new ArrayList<>();


        for (
                String t :
                (tagLine.isEmpty() ? topic : tagLine).split(";")
        ) {

            if (!t.trim().isEmpty()) {

                tags.add(t.trim());
            }
        }


        String text =
                readLine("Question text: ")
                        .replace('|', '/');


        bank.add(
                new Question(
                        id,
                        subject,
                        topic,
                        difficulty,
                        marks,
                        tags,
                        text
                )
        );


        System.out.println();

        System.out.println(
                "Question added successfully."
        );

        System.out.println(
                "Total questions: " + bank.size()
        );
    }


    static void displayAll() {

        printSection("ALL QUESTIONS");


        int number = 1;


        for (Question q : bank.getAll()) {

            System.out.println(
                    "Question " + number++
            );

            printQuestion(q);

            System.out.println();
        }


        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println(
                "Total questions: " + bank.size()
        );
    }


    static void searchById() {

        printSection("SEARCH QUESTION BY ID");


        int id = readInt("Question ID: ");

        Question q = bank.getById(id);


        System.out.println();


        if (q == null) {

            System.out.println(
                    "No question found with ID " + id + "."
            );

            return;
        }


        printQuestion(q);
    }


    static void searchBySubject() {

        printSection("SEARCH QUESTIONS BY SUBJECT");


        String subject = readLine("Subject: ");

        List<Question> res =
                bank.searchBySubject(subject);


        System.out.println();


        if (res.isEmpty()) {

            System.out.println(
                    "No questions found for subject: "
                            + subject
            );

        } else {

            int number = 1;


            for (Question q : res) {

                System.out.println(
                        "Result " + number++
                );

                printQuestion(q);

                System.out.println();
            }
        }


        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println(
                "Questions found: " + res.size()
        );
    }


    static void updateQuestion() {

        printSection("UPDATE QUESTION");


        int id = readInt("Question ID: ");

        Question q = bank.getById(id);


        System.out.println();


        if (q == null) {

            System.out.println(
                    "No question found with ID " + id + "."
            );

            return;
        }


        System.out.println(
                "Current question:"
        );

        System.out.println(
                q.getText()
        );

        System.out.println();


        String newText =
                readLine("New question text: ")
                        .replace('|', '/');


        bank.updateText(
                id,
                newText
        );


        System.out.println();


        System.out.println(
                "Question updated successfully."
        );
    }


    static void deleteQuestion() {

        printSection("DELETE QUESTION");


        int id =
                readInt("Question ID: ");


        boolean deleted =
                bank.delete(id);


        System.out.println();


        if (deleted) {

            System.out.println(
                    "Question " + id
                            + " deleted successfully."
            );

            System.out.println(
                    "Total questions: "
                            + bank.size()
            );

        } else {

            System.out.println(
                    "No question found with ID "
                            + id + "."
            );
        }
    }


    static void keywordSearch() {

        printSection("KEYWORD SEARCH");


        String kw =
                readLine(
                        "Keyword / phrase: "
                ).toLowerCase();


        if (kw.isEmpty()) {

            System.out.println();

            System.out.println(
                    "Keyword cannot be empty."
            );

            return;
        }


        System.out.println();

        System.out.println(
                "Searching for:"
        );

        System.out.println(kw);

        System.out.println();


        int found = 0;

        boolean agree = true;


        for (Question q : bank.getAll()) {

            String t =
                    q.getText().toLowerCase();


            List<Integer> kmp =
                    StringAlgorithms.kmp(
                            t,
                            kw
                    );


            List<Integer> z =
                    StringAlgorithms.zSearch(
                            t,
                            kw
                    );


            List<Integer> rk =
                    StringAlgorithms.rabinKarp(
                            t,
                            kw
                    );


            if (
                    !kmp.equals(z)
                            || !kmp.equals(rk)
            ) {

                agree = false;
            }


            if (!kmp.isEmpty()) {

                found++;


                System.out.println(
                        "--------------------------------------------------------------"
                );

                System.out.println(
                        "Match " + found
                );


                System.out.println(
                        "Question ID : "
                                + q.getId()
                );

                System.out.println(
                        "Subject     : "
                                + q.getSubject()
                );

                System.out.println(
                        "Topic       : "
                                + q.getTopic()
                );

                System.out.println(
                        "Difficulty  : "
                                + q.getDifficulty()
                );

                System.out.println(
                        "Marks       : "
                                + q.getMarks()
                );


                System.out.println(
                        "Question    :"
                );

                System.out.println(
                        q.getText()
                );


                System.out.println(
                        "Match index : "
                                + kmp
                );
            }
        }


        System.out.println(
                "--------------------------------------------------------------"
        );


        System.out.println(
                "Total questions found : "
                        + found
        );


        if (agree) {

            System.out.println(
                    "Algorithm check        : "
                            + "KMP, Z-function and Rabin-Karp agree"
            );

        } else {

            System.out.println(
                    "Algorithm check        : "
                            + "Algorithms DISAGREE"
            );
        }
    }


    static void repeatedPhrases() {

        printSection("REPEATED PHRASES");


        int minLen =
                readInt(
                        "Minimum phrase length (characters): "
                );


        System.out.println();

        System.out.println(
                "Minimum length: "
                        + minLen
                        + " characters"
        );


        System.out.println();

        System.out.println(
                "Searching..."
        );


        System.out.println();


        List<String> res =
                SuffixTools.repeatedPhrases(
                        bank.getAll(),
                        minLen,
                        15
                );


        if (res.isEmpty()) {

            System.out.println(
                    "No repeated phrase found."
            );

        } else {

            int number = 1;


            for (String s : res) {

                System.out.println(
                        "Result " + number++
                );

                System.out.println(s);

                System.out.println();
            }
        }


        System.out.println(
                "--------------------------------------------------------------"
        );

        System.out.println(
                "These pairs are possible near-duplicate questions."
        );
    }


    static void benchmark() {

        printSection(
                "STRING MATCHING BENCHMARK"
        );


        String kw =
                "time complexity";


        List<String> texts =
                new ArrayList<>();


        for (Question q : bank.getAll()) {

            texts.add(
                    q.getText().toLowerCase()
            );
        }


        int reps = 3000;


        System.out.println(
                "TEST 1"
        );


        System.out.println(
                "Keyword           : " + kw
        );


        System.out.println(
                "Question texts    : "
                        + texts.size()
        );


        System.out.println(
                "Repetitions       : "
                        + reps
        );


        System.out.println();


        runAll(
                texts,
                kw,
                reps
        );


        StringBuilder t =
                new StringBuilder();


        StringBuilder p =
                new StringBuilder();


        for (
                int i = 0;
                i < 200000;
                i++
        ) {

            t.append('a');
        }


        for (
                int i = 0;
                i < 1000;
                i++
        ) {

            p.append('a');
        }


        p.append('b');


        System.out.println();


        System.out.println(
                "=============================================================="
        );


        System.out.println(
                "TEST 2 - WORST CASE FOR NAIVE SEARCH"
        );


        System.out.println(
                "=============================================================="
        );


        System.out.println(
                "Text length      : 200000"
        );


        System.out.println(
                "Pattern length   : 1001"
        );


        System.out.println(
                "Text             : repeated 'a'"
        );


        System.out.println(
                "Pattern          : repeated 'a' followed by 'b'"
        );


        System.out.println();


        runAll(
                Collections.singletonList(
                        t.toString()
                ),
                p.toString(),
                1
        );


        System.out.println();


        System.out.println(
                "=============================================================="
        );


        System.out.println(
                "SUMMARY"
        );


        System.out.println(
                "=============================================================="
        );


        System.out.println(
                "Naive search can become O(n * m) in the worst case."
        );


        System.out.println(
                "KMP, Z-function and Rabin-Karp use "
                        + "linear expected/typical processing"
        );


        System.out.println(
                "for this project and avoid the "
                        + "repeated-comparison pattern of naive search."
        );
    }


    static void runAll(
            List<String> texts,
            String pat,
            int reps
    ) {

        String[] names = {
                "Naive",
                "KMP",
                "Z-function",
                "Rabin-Karp"
        };


        for (
                int alg = 0;
                alg < 4;
                alg++
        ) {

            long matches = 0;

            long start =
                    System.nanoTime();


            for (
                    int r = 0;
                    r < reps;
                    r++
            ) {

                for (String s : texts) {

                    List<Integer> res;


                    switch (alg) {

                        case 0:

                            res =
                                    StringAlgorithms.naive(
                                            s,
                                            pat
                                    );

                            break;


                        case 1:

                            res =
                                    StringAlgorithms.kmp(
                                            s,
                                            pat
                                    );

                            break;


                        case 2:

                            res =
                                    StringAlgorithms.zSearch(
                                            s,
                                            pat
                                    );

                            break;


                        default:

                            res =
                                    StringAlgorithms.rabinKarp(
                                            s,
                                            pat
                                    );
                    }


                    matches +=
                            res.size();
                }
            }


            double timeMs =
                    (System.nanoTime() - start)
                            / 1e6;


            System.out.println(
                    "Algorithm        : "
                            + names[alg]
            );


            System.out.printf(
                    "Time             : %.1f ms%n",
                    timeMs
            );


            System.out.println(
                    "Matches counted  : "
                            + matches
            );


            System.out.println();
        }
    }


    static void buildExam() {

        printSection(
                "BUILD EXAM SETS"
        );


        System.out.println(
                "1. Custom requirements"
        );


        System.out.println(
                "2. Demo - feasible"
        );


        System.out.println(
                "3. Demo - not enough questions"
        );


        System.out.println();


        int mode =
                readInt(
                        "Choose option: "
                );


        List<ExamBuilder.Requirement> reqs =
                new ArrayList<>();


        int sets;


        if (mode == 2) {

            sets = 2;


            reqs.add(
                    new ExamBuilder.Requirement(
                            "Data Structures",
                            "Easy",
                            2
                    )
            );


            reqs.add(
                    new ExamBuilder.Requirement(
                            "Stack",
                            "Easy",
                            1
                    )
            );


            reqs.add(
                    new ExamBuilder.Requirement(
                            "Algorithms",
                            "Hard",
                            2
                    )
            );


            reqs.add(
                    new ExamBuilder.Requirement(
                            "Operating Systems",
                            "Medium",
                            2
                    )
            );


        } else if (mode == 3) {

            sets = 3;


            reqs.add(
                    new ExamBuilder.Requirement(
                            "DBMS",
                            "Hard",
                            2
                    )
            );


            reqs.add(
                    new ExamBuilder.Requirement(
                            "Indexing",
                            "Hard",
                            1
                    )
            );


        } else if (mode == 1) {

            sets =
                    readInt(
                            "Number of sets: "
                    );


            System.out.println();

            System.out.println(
                    "Enter each requirement in this format:"
            );


            System.out.println(
                    "<subject or topic> <difficulty> <count>"
            );


            System.out.println(
                    "Example: Data Structures Easy 2"
            );


            System.out.println(
                    "Press Enter on an empty line when finished."
            );


            System.out.println();


            while (true) {

                String line =
                        readLine(
                                "Requirement: "
                        );


                if (line.isEmpty()) {

                    break;
                }


                String[] p =
                        line.trim().split(
                                "\\s+"
                        );


                if (p.length < 3) {

                    System.out.println(
                            "Invalid format."
                    );


                    System.out.println(
                            "Use: <subject or topic> "
                                    + "<difficulty> <count>"
                    );


                    System.out.println();

                    continue;
                }


                String diff =
                        normalizeDifficulty(
                                p[p.length - 2]
                        );


                if (diff == null) {

                    System.out.println(
                            "Difficulty must be "
                                    + "Easy, Medium or Hard."
                    );


                    System.out.println();

                    continue;
                }


                try {

                    int count =
                            Integer.parseInt(
                                    p[p.length - 1]
                            );


                    StringBuilder key =
                            new StringBuilder();


                    for (
                            int i = 0;
                            i < p.length - 2;
                            i++
                    ) {

                        if (i > 0) {

                            key.append(" ");
                        }


                        key.append(p[i]);
                    }


                    reqs.add(
                            new ExamBuilder.Requirement(
                                    key.toString(),
                                    diff,
                                    count
                            )
                    );


                } catch (NumberFormatException e) {

                    System.out.println(
                            "Count must be a number."
                    );


                    System.out.println();
                }
            }


        } else {

            System.out.println(
                    "Invalid option."
            );

            return;
        }


        if (
                reqs.isEmpty()
                        || sets <= 0
        ) {

            System.out.println();

            System.out.println(
                    "Nothing to build."
            );

            return;
        }


        System.out.println();


        System.out.println(
                "--------------------------------------------------------------"
        );


        System.out.println(
                "Exam sets       : " + sets
        );


        System.out.println(
                "Requirements    : " + reqs.size()
        );


        System.out.println(
                "--------------------------------------------------------------"
        );


        System.out.println();


        ExamBuilder.build(
                new ArrayList<>(
                        bank.getAll()
                ),
                reqs,
                sets
        );
    }


    static void printSection(
            String title
    ) {

        System.out.println();

        System.out.println(
                "=============================================================="
        );


        System.out.println(
                "                       " + title
        );


        System.out.println(
                "=============================================================="
        );


        System.out.println();
    }


    static void printQuestion(
            Question q
    ) {

        System.out.println(
                "Question ID : "
                        + q.getId()
        );


        System.out.println(
                "Subject     : "
                        + q.getSubject()
        );


        System.out.println(
                "Topic       : "
                        + q.getTopic()
        );


        System.out.println(
                "Difficulty  : "
                        + q.getDifficulty()
        );


        System.out.println(
                "Marks       : "
                        + q.getMarks()
        );


        System.out.println(
                "Tags        : "
                        + q.getTags()
        );


        System.out.println(
                "Question    :"
        );


        System.out.println(
                q.getText()
        );
    }


    static void saveNow() {

        try {

            bank.save(
                    questionsFile
            );


            System.out.println(
                    "Saved "
                            + bank.size()
                            + " questions to "
                            + questionsFile
            );


        } catch (IOException e) {

            System.out.println(
                    "Save failed: "
                            + e.getMessage()
            );
        }
    }


    static String normalizeDifficulty(
            String s
    ) {

        s =
                s.trim()
                        .toLowerCase();


        if (s.equals("easy")) {

            return "Easy";
        }


        if (s.equals("medium")) {

            return "Medium";
        }


        if (s.equals("hard")) {

            return "Hard";
        }


        return null;
    }


    static String readLine(
            String prompt
    ) {

        System.out.print(
                prompt
        );


        return sc.hasNextLine()
                ? sc.nextLine().trim()
                : "";
    }


    static int readInt(
            String prompt
    ) {

        while (true) {

            System.out.print(
                    prompt
            );


            if (!sc.hasNextLine()) {

                return 0;
            }


            try {

                return Integer.parseInt(
                        sc.nextLine().trim()
                );


            } catch (
                    NumberFormatException e
            ) {

                System.out.println(
                        "Please enter a number."
                );
            }
        }
    }
}