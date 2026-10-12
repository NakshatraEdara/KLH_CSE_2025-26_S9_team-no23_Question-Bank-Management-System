import java.util.*;

/**
 * CO4: builds several exam sets with NO repeated question using max-flow.
 *
 * Graph:   source --(count)--> requirement slot --(1)--> matching question --(1)--> sink
 *  - one slot per (set, requirement);   e.g. Set 1 needs "Data Structures Easy x2"
 *  - question -> sink has capacity 1, so a question can be used in only ONE slot (no repeats across sets)
 * If max-flow == total questions needed, a valid paper exists.
 * Otherwise the min-cut shows which questions are the bottleneck.
 */
public class ExamBuilder {

    public static class Requirement {
        final String key;         // a subject name OR a topic name
        final String difficulty;
        final int count;
        public Requirement(String key, String difficulty, int count) {
            this.key = key; this.difficulty = difficulty; this.count = count;
        }
        public String getKey() { return key; }
        public String getDifficulty() { return difficulty; }
        public int getCount() { return count; }
        boolean matches(Question q) {
            return q.getDifficulty().equalsIgnoreCase(difficulty)
                    && (q.getSubject().equalsIgnoreCase(key) || q.getTopic().equalsIgnoreCase(key));
        }
        @Override public String toString() { return key + " / " + difficulty + " x" + count; }
    }

    public static class AssignedQuestion {
        public final Question question;
        public final Requirement requirement;
        public AssignedQuestion(Question question, Requirement requirement) {
            this.question = question;
            this.requirement = requirement;
        }
    }

    public static class ExamSet {
        public final int setNumber;
        public final int totalMarks;
        public final List<AssignedQuestion> questions;
        public ExamSet(int setNumber, int totalMarks, List<AssignedQuestion> questions) {
            this.setNumber = setNumber;
            this.totalMarks = totalMarks;
            this.questions = questions;
        }
    }

    public static class RequirementReport {
        public final String requirement;
        public final int assigned;
        public final int wanted;
        public final int pool;
        public final String status;
        public RequirementReport(String requirement, int assigned, int wanted, int pool, String status) {
            this.requirement = requirement;
            this.assigned = assigned;
            this.wanted = wanted;
            this.pool = pool;
            this.status = status;
        }
    }

    public static class BuildResult {
        public final boolean success;
        public final int totalNodes;
        public final int totalNeeded;
        public final int maxFlow;
        public final int cutCapacity;
        public final List<ExamSet> sets;
        public final List<Integer> bottleneckQuestionIds;
        public final List<RequirementReport> requirementReports;

        public BuildResult(boolean success, int totalNodes, int totalNeeded, int maxFlow, int cutCapacity,
                           List<ExamSet> sets, List<Integer> bottleneckQuestionIds, List<RequirementReport> requirementReports) {
            this.success = success;
            this.totalNodes = totalNodes;
            this.totalNeeded = totalNeeded;
            this.maxFlow = maxFlow;
            this.cutCapacity = cutCapacity;
            this.sets = sets;
            this.bottleneckQuestionIds = bottleneckQuestionIds;
            this.requirementReports = requirementReports;
        }
    }

    public static BuildResult compute(List<Question> qs, List<Requirement> reqs, int sets) {
        int R = reqs.size(), Q = qs.size(), slots = sets * R;
        int src = 0, slotBase = 1, qBase = 1 + slots, sink = qBase + Q, N = sink + 1;
        MaxFlow mf = new MaxFlow(N);

        int[] srcEdge = new int[slots];
        List<int[]> qEdges = new ArrayList<>();                   // {edgeId, slot, questionIndex}
        int need = 0;
        for (int s = 0; s < sets; s++) {
            for (int r = 0; r < R; r++) {
                int slot = s * R + r;
                srcEdge[slot] = mf.addEdge(src, slotBase + slot, reqs.get(r).count);
                need += reqs.get(r).count;
                for (int j = 0; j < Q; j++) {
                    if (reqs.get(r).matches(qs.get(j))) {
                        qEdges.add(new int[]{mf.addEdge(slotBase + slot, qBase + j, 1), slot, j});
                    }
                }
            }
        }
        for (int j = 0; j < Q; j++) mf.addEdge(qBase + j, sink, 1);

        int flow = mf.maxFlow(src, sink);

        if (flow == need) {
            List<ExamSet> examSets = new ArrayList<>();
            for (int s = 0; s < sets; s++) {
                int marks = 0;
                List<AssignedQuestion> assigned = new ArrayList<>();
                for (int r = 0; r < R; r++) {
                    for (int[] e : qEdges) {
                        if (e[1] == s * R + r && mf.flowOn(e[0]) == 1) {
                            Question q = qs.get(e[2]);
                            assigned.add(new AssignedQuestion(q, reqs.get(r)));
                            marks += q.getMarks();
                        }
                    }
                }
                examSets.add(new ExamSet(s + 1, marks, assigned));
            }
            return new BuildResult(true, N, need, flow, 0, examSets, Collections.emptyList(), Collections.emptyList());
        }

        // Failure - calculate min-cut
        boolean[] reach = mf.reachableFromSource(src);
        List<int[]> cut = mf.minCutEdges(reach);
        int cutSum = 0;
        for (int[] c : cut) cutSum += c[2];

        List<Integer> stuck = new ArrayList<>();
        for (int j = 0; j < Q; j++) if (reach[qBase + j]) stuck.add(qs.get(j).getId());

        List<RequirementReport> reports = new ArrayList<>();
        for (int r = 0; r < R; r++) {
            int got = 0;
            for (int s = 0; s < sets; s++) got += mf.flowOn(srcEdge[s * R + r]);
            int wanted = reqs.get(r).count * sets;
            int pool = 0;
            for (Question q : qs) if (reqs.get(r).matches(q)) pool++;
            String status = got < wanted ? (pool < wanted ? "Bank has too few" : "Competes with other requirements") : "Satisfied";
            reports.add(new RequirementReport(reqs.get(r).toString(), got, wanted, pool, status));
        }

        return new BuildResult(false, N, need, flow, cutSum, Collections.emptyList(), stuck, reports);
    }

    public static void build(List<Question> qs, List<Requirement> reqs, int sets) {
        BuildResult res = compute(qs, reqs, sets);
        System.out.println("Graph: " + res.totalNodes + " nodes.  Total questions needed = " + res.totalNeeded + ",  max-flow = " + res.maxFlow);

        if (res.success) {
            System.out.println("SUCCESS: a valid paper exists for every set, no question repeated.\n");
            for (ExamSet set : res.sets) {
                System.out.println("=== Set " + set.setNumber + " ===");
                for (AssignedQuestion aq : set.questions) {
                    System.out.println("  [" + aq.requirement + "]  " + aq.question);
                }
                System.out.println("  Total marks: " + set.totalMarks + "\n");
            }
            return;
        }

        System.out.println("FAILED: only " + res.maxFlow + " of " + res.totalNeeded + " questions can be assigned.\n");
        System.out.println("Min-cut capacity = " + res.cutCapacity + " (equals max-flow, as the theorem says).");
        System.out.println("Bottleneck: the " + res.bottleneckQuestionIds.size() + " question(s) on the source side of the cut are ALL already used up: " + res.bottleneckQuestionIds);
        System.out.println("\nShortage per requirement (summed over all sets):");
        for (RequirementReport rep : res.requirementReports) {
            System.out.println("  " + rep.requirement + ":  assigned " + rep.assigned + " of " + rep.wanted
                    + "   (matching questions in bank: " + rep.pool + ")"
                    + (!rep.status.equals("Satisfied") ? "   <-- " + rep.status : ""));
        }
        System.out.println("\nFix: add more matching questions to the dataset, lower the counts, or reduce the number of sets.");
    }
}
