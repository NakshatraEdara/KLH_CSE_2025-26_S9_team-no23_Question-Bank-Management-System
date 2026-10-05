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
        boolean matches(Question q) {
            return q.getDifficulty().equalsIgnoreCase(difficulty)
                    && (q.getSubject().equalsIgnoreCase(key) || q.getTopic().equalsIgnoreCase(key));
        }
        @Override public String toString() { return key + " / " + difficulty + " x" + count; }
    }

    public static void build(List<Question> qs, List<Requirement> reqs, int sets) {
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
        System.out.println("Graph: " + N + " nodes.  Total questions needed = " + need + ",  max-flow = " + flow);

        if (flow == need) {
            System.out.println("SUCCESS: a valid paper exists for every set, no question repeated.\n");
            for (int s = 0; s < sets; s++) {
                System.out.println("=== Set " + (s + 1) + " ===");
                int marks = 0;
                for (int r = 0; r < R; r++) {
                    for (int[] e : qEdges) {
                        if (e[1] == s * R + r && mf.flowOn(e[0]) == 1) {
                            Question q = qs.get(e[2]);
                            System.out.println("  [" + reqs.get(r) + "]  " + q);
                            marks += q.getMarks();
                        }
                    }
                }
                System.out.println("  Total marks: " + marks + "\n");
            }
            return;
        }

        // ---------- not enough questions: use the min-cut to explain why ----------
        System.out.println("FAILED: only " + flow + " of " + need + " questions can be assigned.\n");
        boolean[] reach = mf.reachableFromSource(src);
        List<int[]> cut = mf.minCutEdges(reach);
        int cutSum = 0;
        for (int[] c : cut) cutSum += c[2];
        System.out.println("Min-cut capacity = " + cutSum + " (equals max-flow, as the theorem says).");

        List<Integer> stuck = new ArrayList<>();
        for (int j = 0; j < Q; j++) if (reach[qBase + j]) stuck.add(qs.get(j).getId());
        System.out.println("Bottleneck: the " + stuck.size() + " question(s) on the source side of the cut are ALL already used up: " + stuck);

        System.out.println("\nShortage per requirement (summed over all sets):");
        for (int r = 0; r < R; r++) {
            int got = 0;
            for (int s = 0; s < sets; s++) got += mf.flowOn(srcEdge[s * R + r]);
            int wanted = reqs.get(r).count * sets;
            int pool = 0;
            for (Question q : qs) if (reqs.get(r).matches(q)) pool++;
            System.out.println("  " + reqs.get(r) + ":  assigned " + got + " of " + wanted
                    + "   (matching questions in bank: " + pool + ")"
                    + (got < wanted ? (pool < wanted ? "   <-- bank simply has too few" : "   <-- competes with other requirements for the same questions") : ""));
        }
        System.out.println("\nFix: add more matching questions to the dataset, lower the counts, or reduce the number of sets.");
    }
}
