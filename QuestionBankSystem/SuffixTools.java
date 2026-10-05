import java.util.*;

/**
 * CO2: suffix array + LCP array (Kasai), used to find phrases that are repeated
 * in two DIFFERENT questions (near-duplicate detection).
 */
public class SuffixTools {
    private static final char SEP = '\u0001';

    /** Suffix array by prefix doubling: sort suffixes by first 1, 2, 4, 8 ... characters. O(n log^2 n). */
    public static int[] buildSuffixArray(String s) {
        int n = s.length();
        Integer[] sa = new Integer[n];
        int[] rank = new int[n], tmp = new int[n];
        for (int i = 0; i < n; i++) { sa[i] = i; rank[i] = s.charAt(i); }
        for (int k = 1; ; k <<= 1) {
            final int kk = k;
            Comparator<Integer> cmp = (a, b) -> {
                if (rank[a] != rank[b]) return Integer.compare(rank[a], rank[b]);
                int ra = a + kk < n ? rank[a + kk] : -1;
                int rb = b + kk < n ? rank[b + kk] : -1;
                return Integer.compare(ra, rb);
            };
            Arrays.sort(sa, cmp);
            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++) tmp[sa[i]] = tmp[sa[i - 1]] + (cmp.compare(sa[i - 1], sa[i]) < 0 ? 1 : 0);
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[sa[n - 1]] == n - 1) break;      // all ranks distinct -> done
        }
        int[] res = new int[n];
        for (int i = 0; i < n; i++) res[i] = sa[i];
        return res;
    }

    /** Kasai: lcp[i] = longest common prefix of suffixes sa[i-1] and sa[i]. O(n). */
    public static int[] kasai(String s, int[] sa) {
        int n = s.length();
        int[] rank = new int[n], lcp = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        for (int i = 0, h = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[rank[i]] = h;
                if (h > 0) h--;
            } else h = 0;
        }
        return lcp;
    }

    private static class Hit {
        int len, idA, idB; String phrase;
        Hit(int len, int idA, int idB, String phrase) { this.len = len; this.idA = idA; this.idB = idB; this.phrase = phrase; }
    }

    /**
     * Joins all question texts (lower-case, separated by SEP) into one string, builds suffix array + LCP,
     * and reports the longest phrases shared by two different questions.
     * Adjacent suffixes in sorted order share the longest prefixes, so only adjacent pairs are checked.
     */
    public static List<String> repeatedPhrases(List<Question> qs, int minLen, int top) {
        StringBuilder sb = new StringBuilder();
        for (Question q : qs) sb.append(q.getText().toLowerCase()).append(SEP);
        String s = sb.toString();
        int n = s.length();

        int[] owner = new int[n];                     // which question each character belongs to
        for (int qi = 0, pos = 0; qi < qs.size(); qi++) {
            int len = qs.get(qi).getText().length() + 1;
            for (int k = 0; k < len; k++) owner[pos++] = qi;
        }
        int[] dist = new int[n + 1];                  // characters left before the next separator
        for (int i = n - 1; i >= 0; i--) dist[i] = (s.charAt(i) == SEP) ? 0 : dist[i + 1] + 1;

        int[] sa = buildSuffixArray(s);
        int[] lcp = kasai(s, sa);

        List<Hit> hits = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            int a = sa[i - 1], b = sa[i];
            if (owner[a] == owner[b]) continue;       // must come from different questions
            int len = Math.min(lcp[i], Math.min(dist[a], dist[b]));   // never cross a separator
            if (len < minLen) continue;
            // trim the phrase to whole words (drop a half-word at the start / end)
            int start = a, end = a + len;
            if (start > 0 && Character.isLetter(s.charAt(start - 1)) && Character.isLetter(s.charAt(start))) {
                while (start < end && Character.isLetter(s.charAt(start))) start++;
            }
            if (end < n && Character.isLetter(s.charAt(end - 1)) && Character.isLetter(s.charAt(end))) {
                while (end > start && Character.isLetter(s.charAt(end - 1))) end--;
            }
            String phrase = s.substring(start, end).trim();
            if (phrase.length() >= minLen) {
                hits.add(new Hit(phrase.length(), qs.get(owner[a]).getId(), qs.get(owner[b]).getId(), phrase));
            }
        }
        hits.sort((x, y) -> Integer.compare(y.len, x.len));

        List<String> out = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();
        for (Hit h : hits) {
            String key = Math.min(h.idA, h.idB) + "-" + Math.max(h.idA, h.idB);
            if (!seenPairs.add(key)) continue;        // keep only the longest phrase per pair
            out.add("Q" + h.idA + " & Q" + h.idB + "  (" + h.len + " chars): \"" + h.phrase + "\"");
            if (out.size() == top) break;
        }
        return out;
    }
}
