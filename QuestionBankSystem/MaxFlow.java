import java.util.*;

/**
 * CO4: Edmonds-Karp = Ford-Fulkerson where each augmenting path is found by BFS
 * (shortest path in number of edges). Complexity O(V * E^2).
 * Edges are stored in pairs: edge e and its reverse edge e^1 (residual graph).
 */
public class MaxFlow {
    private final int n;
    private final List<List<Integer>> adj = new ArrayList<>();
    private final List<Integer> to = new ArrayList<>();
    private final List<Integer> cap = new ArrayList<>();      // residual capacity
    private final List<Integer> origCap = new ArrayList<>();  // capacity when created

    public MaxFlow(int n) {
        this.n = n;
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    /** Adds u -> v with capacity c (and a reverse edge with 0 capacity). Returns the forward edge id. */
    public int addEdge(int u, int v, int c) {
        int id = to.size();
        to.add(v); cap.add(c); origCap.add(c); adj.get(u).add(id);
        to.add(u); cap.add(0); origCap.add(0); adj.get(v).add(id + 1);
        return id;
    }

    /** Flow currently sent along forward edge e = capacity of its reverse edge. */
    public int flowOn(int e) { return cap.get(e ^ 1); }

    public int maxFlow(int s, int t) {
        int flow = 0;
        while (true) {
            int[] prevEdge = new int[n];
            Arrays.fill(prevEdge, -1);
            boolean[] seen = new boolean[n];
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(s); seen[s] = true;
            while (!queue.isEmpty() && !seen[t]) {                 // BFS for an augmenting path
                int u = queue.poll();
                for (int e : adj.get(u)) {
                    int v = to.get(e);
                    if (cap.get(e) > 0 && !seen[v]) { seen[v] = true; prevEdge[v] = e; queue.add(v); }
                }
            }
            if (!seen[t]) break;                                   // no path left -> flow is maximum

            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = to.get(prevEdge[v] ^ 1)) bottleneck = Math.min(bottleneck, cap.get(prevEdge[v]));
            for (int v = t; v != s; v = to.get(prevEdge[v] ^ 1)) {
                int e = prevEdge[v];
                cap.set(e, cap.get(e) - bottleneck);
                cap.set(e ^ 1, cap.get(e ^ 1) + bottleneck);
            }
            flow += bottleneck;
        }
        return flow;
    }

    /** After maxFlow: nodes still reachable from s in the residual graph = source side of the min-cut. */
    public boolean[] reachableFromSource(int s) {
        boolean[] seen = new boolean[n];
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(s); seen[s] = true;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int e : adj.get(u)) {
                int v = to.get(e);
                if (cap.get(e) > 0 && !seen[v]) { seen[v] = true; queue.add(v); }
            }
        }
        return seen;
    }

    /** Forward edges going from the source side to the sink side: {from, to, capacity}. Their capacities add up to the max-flow. */
    public List<int[]> minCutEdges(boolean[] reach) {
        List<int[]> cut = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            if (!reach[u]) continue;
            for (int e : adj.get(u)) {
                if (e % 2 == 0 && origCap.get(e) > 0 && !reach[to.get(e)]) cut.add(new int[]{u, to.get(e), origCap.get(e)});
            }
        }
        return cut;
    }
}
