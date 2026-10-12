package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Question;
import store.QuestionBank;
import algorithms.StringAlgorithms;
import algorithms.SuffixTools;
import exam.ExamBuilder;

/**
 * Aesthetic, High-Performance Web Dashboard Server for QuestionBankSystem.
 * Built entirely with Java's standard library (zero external dependencies).
 */
public class DashboardServer {
    private static final int DEFAULT_PORT = 8080;
    private static final String QUESTIONS_DIR = "data/questions";
    private static final String WEB_DIR = "web";

    private final QuestionBank bank = new QuestionBank();
    private final int port;
    private HttpServer server;

    public DashboardServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        DashboardServer app = new DashboardServer(port);
        try {
            app.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    public void start() throws IOException {
        int loaded = bank.load(QUESTIONS_DIR);
        System.out.println("=================================================");
        System.out.println("  DSA QUESTION BANK - EXECUTIVE DASHBOARD SERVER ");
        System.out.println("=================================================");
        System.out.println("Loaded " + loaded + " questions from '" + QUESTIONS_DIR + "'");

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API endpoints
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/questions", new QuestionsHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/phrases", new PhrasesHandler());
        server.createContext("/api/benchmark", new BenchmarkHandler());
        server.createContext("/api/exam/build", new ExamHandler());
        server.createContext("/api/save", new SaveHandler());

        // Static file serving for web UI
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        System.out.println("Dashboard running at: http://localhost:" + port + "/");
        System.out.println("Press Ctrl+C to stop.");
    }

    // =========================================================================
    //  STATIC FILE HANDLER
    // =========================================================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                path = "/index.html";
            }

            // Security check against directory traversal
            if (path.contains("..")) {
                sendResponse(exchange, 403, "text/plain", "Forbidden");
                return;
            }

            Path filePath = Paths.get(WEB_DIR, path.startsWith("/") ? path.substring(1) : path);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                // Fallback to index.html for SPA routing if available
                Path indexPath = Paths.get(WEB_DIR, "index.html");
                if (Files.exists(indexPath)) {
                    filePath = indexPath;
                } else {
                    sendResponse(exchange, 404, "text/plain", "404 Not Found");
                    return;
                }
            }

            String mime = getMimeType(filePath.getFileName().toString());
            byte[] bytes = Files.readAllBytes(filePath);

            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================================
    //  API HANDLERS
    // =========================================================================

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, Integer> topicCounts = new TreeMap<>();
            Map<String, Integer> diffCounts = new LinkedHashMap<>();
            diffCounts.put("Easy", 0);
            diffCounts.put("Medium", 0);
            diffCounts.put("Hard", 0);
            Map<Integer, Integer> marksCounts = new TreeMap<>();
            int totalMarks = 0;

            List<Question> all = bank.getAll();
            for (Question q : all) {
                topicCounts.put(q.getTopic(), topicCounts.getOrDefault(q.getTopic(), 0) + 1);
                String d = q.getDifficulty();
                if (d != null) {
                    diffCounts.put(d, diffCounts.getOrDefault(d, 0) + 1);
                }
                marksCounts.put(q.getMarks(), marksCounts.getOrDefault(q.getMarks(), 0) + 1);
                totalMarks += q.getMarks();
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"totalQuestions\":").append(all.size()).append(",");
            json.append("\"totalMarks\":").append(totalMarks).append(",");
            json.append("\"avgMarks\":").append(all.isEmpty() ? 0 : String.format(Locale.US, "%.2f", (double) totalMarks / all.size())).append(",");
            json.append("\"uniqueTopics\":").append(topicCounts.size()).append(",");

            // difficulties
            json.append("\"difficulties\":{");
            boolean first = true;
            for (Map.Entry<String, Integer> e : diffCounts.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(escapeJson(e.getKey())).append("\":").append(e.getValue());
            }
            json.append("},");

            // marks
            json.append("\"marks\":{");
            first = true;
            for (Map.Entry<Integer, Integer> e : marksCounts.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            }
            json.append("},");

            // topics
            json.append("\"topics\":{");
            first = true;
            for (Map.Entry<String, Integer> e : topicCounts.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("\"").append(escapeJson(e.getKey())).append("\":").append(e.getValue());
            }
            json.append("}");

            json.append("}");

            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toString());
        }
    }

    private class QuestionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String idStr = null;
            if (path.startsWith("/api/questions/")) {
                idStr = path.substring("/api/questions/".length()).trim();
            }

            if ("GET".equalsIgnoreCase(method)) {
                if (idStr != null && !idStr.isEmpty()) {
                    handleGetOne(exchange, idStr);
                } else {
                    handleGetList(exchange);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                handleCreate(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handleUpdate(exchange, idStr);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange, idStr);
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }

        private void handleGetOne(HttpExchange exchange, String idStr) throws IOException {
            try {
                int id = Integer.parseInt(idStr);
                Question q = bank.getById(id);
                if (q == null) {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Question not found\"}");
                    return;
                }
                sendResponse(exchange, 200, "application/json; charset=utf-8", questionToJson(q));
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Invalid ID\"}");
            }
        }

        private void handleGetList(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String search = query.getOrDefault("q", "").toLowerCase();
            String topic = query.getOrDefault("topic", "");
            String difficulty = query.getOrDefault("difficulty", "");
            String marksStr = query.getOrDefault("marks", "");
            int marksFilter = -1;
            if (!marksStr.isEmpty()) {
                try { marksFilter = Integer.parseInt(marksStr); } catch (NumberFormatException ignored) {}
            }

            int page = 1;
            try { page = Math.max(1, Integer.parseInt(query.getOrDefault("page", "1"))); } catch (Exception ignored) {}
            int limit = 15;
            try { limit = Integer.parseInt(query.getOrDefault("limit", "15")); } catch (Exception ignored) {}

            List<Question> filtered = new ArrayList<>();
            for (Question q : bank.getAll()) {
                if (!topic.isEmpty() && !q.getTopic().equalsIgnoreCase(topic)) continue;
                if (!difficulty.isEmpty() && !q.getDifficulty().equalsIgnoreCase(difficulty)) continue;
                if (marksFilter != -1 && q.getMarks() != marksFilter) continue;
                if (!search.isEmpty()) {
                    String full = (q.getText() + " " + q.getTopic() + " " + q.getSubject() + " " + q.getId()).toLowerCase();
                    if (!full.contains(search)) continue;
                }
                filtered.add(q);
            }

            int total = filtered.size();
            int totalPages = limit > 0 ? (int) Math.ceil((double) total / limit) : 1;
            if (totalPages == 0) totalPages = 1;

            int fromIndex = limit > 0 ? Math.min((page - 1) * limit, total) : 0;
            int toIndex = limit > 0 ? Math.min(fromIndex + limit, total) : total;

            List<Question> pageItems = filtered.subList(fromIndex, toIndex);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"total\":").append(total).append(",");
            json.append("\"page\":").append(page).append(",");
            json.append("\"totalPages\":").append(totalPages).append(",");
            json.append("\"limit\":").append(limit).append(",");
            json.append("\"questions\":[");
            for (int i = 0; i < pageItems.size(); i++) {
                if (i > 0) json.append(",");
                json.append(questionToJson(pageItems.get(i)));
            }
            json.append("]}");

            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toString());
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            Map<String, String> data = parseSimpleJson(body);

            int nextId = 1;
            for (Question q : bank.getAll()) {
                if (q.getId() >= nextId) nextId = q.getId() + 1;
            }

            if (data.containsKey("id") && !data.get("id").isEmpty()) {
                try {
                    int customId = Integer.parseInt(data.get("id"));
                    if (!bank.contains(customId)) nextId = customId;
                } catch (NumberFormatException ignored) {}
            }

            String subject = data.getOrDefault("subject", "Data Structures and Algorithms");
            String topic = data.getOrDefault("topic", "General");
            String difficulty = data.getOrDefault("difficulty", "Medium");
            int marks = 5;
            try { marks = Integer.parseInt(data.getOrDefault("marks", "5")); } catch (NumberFormatException ignored) {}

            String stem = data.getOrDefault("stem", "");
            String optA = data.getOrDefault("optA", "");
            String optB = data.getOrDefault("optB", "");
            String optC = data.getOrDefault("optC", "");
            String optD = data.getOrDefault("optD", "");
            String ans = data.getOrDefault("correctAnswer", "A");
            String exp = data.getOrDefault("explanation", "");

            // Build full raw text for saving
            StringBuilder fullText = new StringBuilder();
            fullText.append("Question ID: ").append(nextId).append("\n");
            fullText.append("Subject: ").append(subject).append("\n");
            fullText.append("Topic: ").append(topic).append("\n");
            fullText.append("Difficulty: ").append(difficulty).append("\n");
            fullText.append("Question Type: Multiple Choice\n\n");
            fullText.append("Question:\n").append(stem).append("\n\n");
            fullText.append("Options:\n");
            fullText.append("A. ").append(optA).append("\n");
            fullText.append("B. ").append(optB).append("\n");
            fullText.append("C. ").append(optC).append("\n");
            fullText.append("D. ").append(optD).append("\n\n");
            fullText.append("Correct Answer: ").append(ans).append("\n\n");
            fullText.append("Explanation:\n").append(exp).append("\n\n");
            fullText.append("Marks: ").append(marks).append("\n");

            Question newQ = new Question(nextId, subject, topic, difficulty, marks, Collections.singletonList(topic), fullText.toString());
            boolean ok = bank.add(newQ);

            if (ok) {
                saveSingleQuestionFile(newQ);
                sendResponse(exchange, 201, "application/json; charset=utf-8", questionToJson(newQ));
            } else {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Could not add question (duplicate ID)\"}");
            }
        }

        private void handleUpdate(HttpExchange exchange, String idStr) throws IOException {
            if (idStr == null || idStr.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Missing ID\"}");
                return;
            }
            try {
                int id = Integer.parseInt(idStr);
                Question existing = bank.getById(id);
                if (existing == null) {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Question not found\"}");
                    return;
                }

                String body = readBody(exchange);
                Map<String, String> data = parseSimpleJson(body);

                String subject = data.getOrDefault("subject", existing.getSubject());
                String topic = data.getOrDefault("topic", existing.getTopic());
                String difficulty = data.getOrDefault("difficulty", existing.getDifficulty());
                int marks = existing.getMarks();
                if (data.containsKey("marks")) {
                    try { marks = Integer.parseInt(data.get("marks")); } catch (NumberFormatException ignored) {}
                }

                String stem = data.containsKey("stem") ? data.get("stem") : extractStem(existing.getText());
                String optA = data.getOrDefault("optA", "");
                String optB = data.getOrDefault("optB", "");
                String optC = data.getOrDefault("optC", "");
                String optD = data.getOrDefault("optD", "");
                String ans = data.getOrDefault("correctAnswer", extractAnswer(existing.getText()));
                String exp = data.getOrDefault("explanation", extractExplanation(existing.getText()));

                // If options not provided separately, parse existing
                List<String> existingOpts = extractOptions(existing.getText());
                if (optA.isEmpty() && existingOpts.size() > 0) optA = cleanOption(existingOpts.get(0));
                if (optB.isEmpty() && existingOpts.size() > 1) optB = cleanOption(existingOpts.get(1));
                if (optC.isEmpty() && existingOpts.size() > 2) optC = cleanOption(existingOpts.get(2));
                if (optD.isEmpty() && existingOpts.size() > 3) optD = cleanOption(existingOpts.get(3));

                StringBuilder fullText = new StringBuilder();
                fullText.append("Question ID: ").append(id).append("\n");
                fullText.append("Subject: ").append(subject).append("\n");
                fullText.append("Topic: ").append(topic).append("\n");
                fullText.append("Difficulty: ").append(difficulty).append("\n");
                fullText.append("Question Type: Multiple Choice\n\n");
                fullText.append("Question:\n").append(stem).append("\n\n");
                fullText.append("Options:\n");
                fullText.append("A. ").append(optA).append("\n");
                fullText.append("B. ").append(optB).append("\n");
                fullText.append("C. ").append(optC).append("\n");
                fullText.append("D. ").append(optD).append("\n\n");
                fullText.append("Correct Answer: ").append(ans).append("\n\n");
                fullText.append("Explanation:\n").append(exp).append("\n\n");
                fullText.append("Marks: ").append(marks).append("\n");

                Question updated = new Question(id, subject, topic, difficulty, marks, Collections.singletonList(topic), fullText.toString());
                bank.replace(id, updated);
                saveSingleQuestionFile(updated);

                sendResponse(exchange, 200, "application/json; charset=utf-8", questionToJson(updated));
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Invalid ID\"}");
            }
        }

        private void handleDelete(HttpExchange exchange, String idStr) throws IOException {
            if (idStr == null || idStr.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Missing ID\"}");
                return;
            }
            try {
                int id = Integer.parseInt(idStr);
                boolean deleted = bank.delete(id);
                if (deleted) {
                    Path file = Paths.get(QUESTIONS_DIR, String.format("question_%03d.txt", id));
                    Files.deleteIfExists(file);
                    sendResponse(exchange, 200, "application/json", "{\"success\":true,\"id\":" + id + "}");
                } else {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Question not found\"}");
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Invalid ID\"}");
            }
        }
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String kw = query.getOrDefault("q", "").trim().toLowerCase();

            if (kw.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Keyword 'q' cannot be empty\"}");
                return;
            }

            List<Question> all = bank.getAll();
            long t0 = System.nanoTime();
            int naiveCount = 0;
            for (Question q : all) {
                naiveCount += StringAlgorithms.naive(q.getText().toLowerCase(), kw).size();
            }
            double naiveMs = (System.nanoTime() - t0) / 1e6;

            t0 = System.nanoTime();
            int kmpCount = 0;
            List<Map<String, Object>> matches = new ArrayList<>();
            boolean agree = true;

            for (Question q : all) {
                String t = q.getText().toLowerCase();
                List<Integer> kmpHits = StringAlgorithms.kmp(t, kw);
                List<Integer> zHits = StringAlgorithms.zSearch(t, kw);
                List<Integer> rkHits = StringAlgorithms.rabinKarp(t, kw);

                if (!kmpHits.equals(zHits) || !kmpHits.equals(rkHits)) {
                    agree = false;
                }

                if (!kmpHits.isEmpty()) {
                    kmpCount += kmpHits.size();
                    Map<String, Object> hit = new LinkedHashMap<>();
                    hit.put("questionId", q.getId());
                    hit.put("subject", q.getSubject());
                    hit.put("topic", q.getTopic());
                    hit.put("difficulty", q.getDifficulty());
                    hit.put("marks", q.getMarks());
                    hit.put("stem", extractStem(q.getText()));
                    hit.put("indices", kmpHits);
                    matches.add(hit);
                }
            }
            double kmpMs = (System.nanoTime() - t0) / 1e6;

            t0 = System.nanoTime();
            for (Question q : all) {
                StringAlgorithms.zSearch(q.getText().toLowerCase(), kw);
            }
            double zMs = (System.nanoTime() - t0) / 1e6;

            t0 = System.nanoTime();
            for (Question q : all) {
                StringAlgorithms.rabinKarp(q.getText().toLowerCase(), kw);
            }
            double rkMs = (System.nanoTime() - t0) / 1e6;

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"query\":\"").append(escapeJson(kw)).append("\",");
            json.append("\"totalQuestionsMatched\":").append(matches.size()).append(",");
            json.append("\"totalMatchesFound\":").append(kmpCount).append(",");
            json.append("\"algorithmsAgree\":").append(agree).append(",");
            json.append("\"timingMs\":{");
            json.append("\"naive\":").append(String.format(Locale.US, "%.3f", naiveMs)).append(",");
            json.append("\"kmp\":").append(String.format(Locale.US, "%.3f", kmpMs)).append(",");
            json.append("\"zSearch\":").append(String.format(Locale.US, "%.3f", zMs)).append(",");
            json.append("\"rabinKarp\":").append(String.format(Locale.US, "%.3f", rkMs));
            json.append("},");
            json.append("\"matches\":[");
            for (int i = 0; i < matches.size(); i++) {
                if (i > 0) json.append(",");
                Map<String, Object> m = matches.get(i);
                json.append("{");
                json.append("\"questionId\":").append(m.get("questionId")).append(",");
                json.append("\"subject\":\"").append(escapeJson((String) m.get("subject"))).append("\",");
                json.append("\"topic\":\"").append(escapeJson((String) m.get("topic"))).append("\",");
                json.append("\"difficulty\":\"").append(escapeJson((String) m.get("difficulty"))).append("\",");
                json.append("\"marks\":").append(m.get("marks")).append(",");
                json.append("\"stem\":\"").append(escapeJson((String) m.get("stem"))).append("\",");
                @SuppressWarnings("unchecked")
                List<Integer> idxs = (List<Integer>) m.get("indices");
                json.append("\"indices\":").append(idxs.toString());
                json.append("}");
            }
            json.append("]}");

            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toString());
        }
    }

    private class PhrasesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            int minLen = 25;
            int top = 20;
            try { minLen = Math.max(5, Integer.parseInt(query.getOrDefault("minLen", "25"))); } catch (Exception ignored) {}
            try { top = Math.max(1, Integer.parseInt(query.getOrDefault("top", "20"))); } catch (Exception ignored) {}

            List<SuffixTools.PhraseHit> hits = SuffixTools.detectPhrases(bank.getAll(), minLen, top);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"minLen\":").append(minLen).append(",");
            json.append("\"count\":").append(hits.size()).append(",");
            json.append("\"hits\":[");
            for (int i = 0; i < hits.size(); i++) {
                if (i > 0) json.append(",");
                SuffixTools.PhraseHit h = hits.get(i);
                Question qA = bank.getById(h.idA);
                Question qB = bank.getById(h.idB);
                json.append("{");
                json.append("\"length\":").append(h.len).append(",");
                json.append("\"idA\":").append(h.idA).append(",");
                json.append("\"idB\":").append(h.idB).append(",");
                json.append("\"topicA\":\"").append(escapeJson(qA != null ? qA.getTopic() : "")).append("\",");
                json.append("\"topicB\":\"").append(escapeJson(qB != null ? qB.getTopic() : "")).append("\",");
                json.append("\"phrase\":\"").append(escapeJson(h.phrase)).append("\"");
                json.append("}");
            }
            json.append("]}");

            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toString());
        }
    }

    private class BenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            int reps = 1000;
            try { reps = Math.max(10, Math.min(5000, Integer.parseInt(query.getOrDefault("reps", "1000")))); } catch (Exception ignored) {}

            String kw = "time complexity";
            List<String> texts = new ArrayList<>();
            for (Question q : bank.getAll()) {
                texts.add(q.getText().toLowerCase());
            }

            // Test 1: Real corpus
            double tNaive = benchmarkAlg(texts, kw, reps, 0);
            double tKmp = benchmarkAlg(texts, kw, reps, 1);
            double tZ = benchmarkAlg(texts, kw, reps, 2);
            double tRk = benchmarkAlg(texts, kw, reps, 3);

            // Test 2: Pathological worst-case
            StringBuilder sbT = new StringBuilder();
            StringBuilder sbP = new StringBuilder();
            for (int i = 0; i < 150000; i++) sbT.append('a');
            for (int i = 0; i < 500; i++) sbP.append('a');
            sbP.append('b');
            List<String> badText = Collections.singletonList(sbT.toString());
            String badPat = sbP.toString();

            double tBadNaive = benchmarkAlg(badText, badPat, 1, 0);
            double tBadKmp = benchmarkAlg(badText, badPat, 1, 1);
            double tBadZ = benchmarkAlg(badText, badPat, 1, 2);
            double tBadRk = benchmarkAlg(badText, badPat, 1, 3);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"reps\":").append(reps).append(",");
            json.append("\"test1\":{");
            json.append("\"corpusSize\":").append(texts.size()).append(",");
            json.append("\"keyword\":\"").append(escapeJson(kw)).append("\",");
            json.append("\"naiveMs\":").append(String.format(Locale.US, "%.1f", tNaive)).append(",");
            json.append("\"kmpMs\":").append(String.format(Locale.US, "%.1f", tKmp)).append(",");
            json.append("\"zMs\":").append(String.format(Locale.US, "%.1f", tZ)).append(",");
            json.append("\"rkMs\":").append(String.format(Locale.US, "%.1f", tRk));
            json.append("},");
            json.append("\"test2\":{");
            json.append("\"textLength\":150000,");
            json.append("\"patternLength\":501,");
            json.append("\"naiveMs\":").append(String.format(Locale.US, "%.1f", tBadNaive)).append(",");
            json.append("\"kmpMs\":").append(String.format(Locale.US, "%.1f", tBadKmp)).append(",");
            json.append("\"zMs\":").append(String.format(Locale.US, "%.1f", tBadZ)).append(",");
            json.append("\"rkMs\":").append(String.format(Locale.US, "%.1f", tBadRk));
            json.append("}");
            json.append("}");

            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toString());
        }

        private double benchmarkAlg(List<String> texts, String pat, int reps, int alg) {
            long start = System.nanoTime();
            for (int r = 0; r < reps; r++) {
                for (String s : texts) {
                    switch (alg) {
                        case 0: StringAlgorithms.naive(s, pat); break;
                        case 1: StringAlgorithms.kmp(s, pat); break;
                        case 2: StringAlgorithms.zSearch(s, pat); break;
                        default: StringAlgorithms.rabinKarp(s, pat); break;
                    }
                }
            }
            return (System.nanoTime() - start) / 1e6;
        }
    }

    private class ExamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            // Parse requirements from JSON
            int sets = 2;
            List<ExamBuilder.Requirement> reqs = new ArrayList<>();

            Matcher setsMatcher = Pattern.compile("\"sets\"\\s*:\\s*(\\d+)").matcher(body);
            if (setsMatcher.find()) {
                sets = Integer.parseInt(setsMatcher.group(1));
            }

            Pattern reqPattern = Pattern.compile("\\{\\s*\"key\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"difficulty\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"count\"\\s*:\\s*(\\d+)\\s*\\}");
            Matcher rm = reqPattern.matcher(body);
            while (rm.find()) {
                String key = rm.group(1);
                String diff = rm.group(2);
                int count = Integer.parseInt(rm.group(3));
                reqs.add(new ExamBuilder.Requirement(key, diff, count));
            }

            if (reqs.isEmpty()) {
                // Default fallback preset
                reqs.add(new ExamBuilder.Requirement("Data Structures and Algorithms", "Easy", 2));
                reqs.add(new ExamBuilder.Requirement("Dynamic Programming", "Medium", 2));
                reqs.add(new ExamBuilder.Requirement("Maximum Flow", "Hard", 1));
            }

            ExamBuilder.BuildResult res = ExamBuilder.compute(bank.getAll(), reqs, sets);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"success\":").append(res.success).append(",");
            json.append("\"setsRequested\":").append(sets).append(",");
            json.append("\"totalNodes\":").append(res.totalNodes).append(",");
            json.append("\"totalNeeded\":").append(res.totalNeeded).append(",");
            json.append("\"maxFlow\":").append(res.maxFlow).append(",");
            json.append("\"cutCapacity\":").append(res.cutCapacity).append(",");

            // Sets
            json.append("\"sets\":[");
            for (int s = 0; s < res.sets.size(); s++) {
                if (s > 0) json.append(",");
                ExamBuilder.ExamSet set = res.sets.get(s);
                json.append("{");
                json.append("\"setNumber\":").append(set.setNumber).append(",");
                json.append("\"totalMarks\":").append(set.totalMarks).append(",");
                json.append("\"questions\":[");
                for (int q = 0; q < set.questions.size(); q++) {
                    if (q > 0) json.append(",");
                    ExamBuilder.AssignedQuestion aq = set.questions.get(q);
                    json.append("{");
                    json.append("\"id\":").append(aq.question.getId()).append(",");
                    json.append("\"subject\":\"").append(escapeJson(aq.question.getSubject())).append("\",");
                    json.append("\"topic\":\"").append(escapeJson(aq.question.getTopic())).append("\",");
                    json.append("\"difficulty\":\"").append(escapeJson(aq.question.getDifficulty())).append("\",");
                    json.append("\"marks\":").append(aq.question.getMarks()).append(",");
                    json.append("\"requirement\":\"").append(escapeJson(aq.requirement.toString())).append("\",");
                    json.append("\"stem\":\"").append(escapeJson(extractStem(aq.question.getText()))).append("\",");
                    json.append("\"options\":").append(optionsToJson(extractOptions(aq.question.getText()))).append(",");
                    json.append("\"correctAnswer\":\"").append(escapeJson(extractAnswer(aq.question.getText()))).append("\",");
                    json.append("\"explanation\":\"").append(escapeJson(extractExplanation(aq.question.getText()))).append("\"");
                    json.append("}");
                }
                json.append("]}");
            }
            json.append("],");

            // Bottlenecks
            json.append("\"bottleneckQuestionIds\":").append(res.bottleneckQuestionIds.toString()).append(",");
            json.append("\"reports\":[");
            for (int r = 0; r < res.requirementReports.size(); r++) {
                if (r > 0) json.append(",");
                ExamBuilder.RequirementReport rr = res.requirementReports.get(r);
                json.append("{");
                json.append("\"requirement\":\"").append(escapeJson(rr.requirement)).append("\",");
                json.append("\"assigned\":").append(rr.assigned).append(",");
                json.append("\"wanted\":").append(rr.wanted).append(",");
                json.append("\"pool\":").append(rr.pool).append(",");
                json.append("\"status\":\"").append(escapeJson(rr.status)).append("\"");
                json.append("}");
            }
            json.append("]");

            json.append("}");

            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toString());
        }
    }

    private class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                bank.save(QUESTIONS_DIR);
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"total\":" + bank.size() + "}");
            } catch (IOException e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    //  HELPERS: SERIALIZATION & PARSING
    // =========================================================================

    private String questionToJson(Question q) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(q.getId()).append(",");
        sb.append("\"subject\":\"").append(escapeJson(q.getSubject())).append("\",");
        sb.append("\"topic\":\"").append(escapeJson(q.getTopic())).append("\",");
        sb.append("\"difficulty\":\"").append(escapeJson(q.getDifficulty())).append("\",");
        sb.append("\"marks\":").append(q.getMarks()).append(",");
        sb.append("\"stem\":\"").append(escapeJson(extractStem(q.getText()))).append("\",");
        sb.append("\"options\":").append(optionsToJson(extractOptions(q.getText()))).append(",");
        sb.append("\"correctAnswer\":\"").append(escapeJson(extractAnswer(q.getText()))).append("\",");
        sb.append("\"explanation\":\"").append(escapeJson(extractExplanation(q.getText()))).append("\",");
        sb.append("\"tags\":[");
        for (int i = 0; i < q.getTags().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(q.getTags().get(i))).append("\"");
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private String optionsToJson(List<String> options) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(options.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String cleanOption(String opt) {
        if (opt.matches("^[A-D]\\.\\s*.*")) {
            return opt.replaceFirst("^[A-D]\\.\\s*", "");
        }
        return opt;
    }

    private void saveSingleQuestionFile(Question q) throws IOException {
        Path dir = Paths.get(QUESTIONS_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path file = dir.resolve(String.format("question_%03d.txt", q.getId()));
        Files.write(file, q.getText().getBytes(StandardCharsets.UTF_8));
    }

    private static String extractStem(String raw) {
        Matcher m = Pattern.compile("(?s)Question:\\s*\\n(.*?)(?=\\nOptions:|\\nMarks:|$)").matcher(raw);
        if (m.find()) return m.group(1).trim();
        return raw;
    }

    private static List<String> extractOptions(String raw) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("(?s)Options:\\s*\\n(.*?)(?=\\nCorrect Answer:|\\nMarks:|$)").matcher(raw);
        if (m.find()) {
            for (String line : m.group(1).split("\\r?\\n")) {
                line = line.trim();
                if (!line.isEmpty()) list.add(line);
            }
        }
        return list;
    }

    private static String extractAnswer(String raw) {
        Matcher m = Pattern.compile("Correct Answer:\\s*([A-D])").matcher(raw);
        if (m.find()) return m.group(1).trim();
        return "";
    }

    private static String extractExplanation(String raw) {
        Matcher m = Pattern.compile("(?s)Explanation:\\s*\\n(.*?)(?=\\nMarks:|$)").matcher(raw);
        if (m.find()) return m.group(1).trim();
        return "";
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(\\d+)|(true|false))");
        Matcher m = p.matcher(json);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2) != null ? m.group(2) : (m.group(3) != null ? m.group(3) : m.group(4));
            map.put(key, val);
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    map.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } else if (idx < 0) {
                    map.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (UnsupportedEncodingException ignored) {}
        }
        return map;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
        if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
        if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (fileName.endsWith(".json")) return "application/json; charset=utf-8";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
