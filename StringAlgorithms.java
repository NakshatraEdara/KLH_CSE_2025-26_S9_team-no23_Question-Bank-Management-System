import java.util.*;

/**
 * CO2: linear-time pattern matching, all written by hand (no String.contains / indexOf).
 * Every method returns the list of start positions where pat occurs in text.
 * Callers lower-case both strings first to get case-insensitive search.
 */
public class StringAlgorithms {

    /** Baseline for CO1 comparison: try every start position. O(n*m) worst case. */
    public static List<Integer> naive(String text, String pat) {
        List<Integer> res = new ArrayList<>();
        int n = text.length(), m = pat.length();
        if (m == 0) return res;
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && text.charAt(i + j) == pat.charAt(j)) j++;
            if (j == m) res.add(i);
        }
        return res;
    }

    /** KMP: prefix function (lps) lets the pattern pointer fall back instead of the text pointer. O(n+m). */
    public static List<Integer> kmp(String text, String pat) {
        List<Integer> res = new ArrayList<>();
        int n = text.length(), m = pat.length();
        if (m == 0) return res;
        int[] lps = new int[m];                       // lps[i] = longest proper prefix of pat[0..i] that is also a suffix
        for (int i = 1, k = 0; i < m; i++) {
            while (k > 0 && pat.charAt(i) != pat.charAt(k)) k = lps[k - 1];
            if (pat.charAt(i) == pat.charAt(k)) k++;
            lps[i] = k;
        }
        for (int i = 0, k = 0; i < n; i++) {
            while (k > 0 && text.charAt(i) != pat.charAt(k)) k = lps[k - 1];
            if (text.charAt(i) == pat.charAt(k)) k++;
            if (k == m) { res.add(i - m + 1); k = lps[k - 1]; }
        }
        return res;
    }

    /** Z-function: z[i] = length of longest substring starting at i that matches a prefix of s. O(n). */
    public static int[] zFunction(String s) {
        int n = s.length();
        int[] z = new int[n];
        for (int i = 1, l = 0, r = 0; i < n; i++) {
            if (i < r) z[i] = Math.min(r - i, z[i - l]);
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
            if (i + z[i] > r) { l = i; r = i + z[i]; }
        }
        return z;
    }

    /** Search using Z-function on  pattern + separator + text. Any z[i] == m is a match. O(n+m). */
    public static List<Integer> zSearch(String text, String pat) {
        List<Integer> res = new ArrayList<>();
        int m = pat.length();
        if (m == 0) return res;
        String s = pat + '\u0001' + text;             // separator never appears in real text
        int[] z = zFunction(s);
        for (int i = m + 1; i < s.length(); i++) {
            if (z[i] == m) res.add(i - m - 1);
        }
        return res;
    }

    private static final long MOD = 1_000_000_007L;
    private static final long BASE = 257;

    /** Rabin-Karp: rolling hash of each window; compare characters only when hashes are equal. Expected O(n+m). */
    public static List<Integer> rabinKarp(String text, String pat) {
        List<Integer> res = new ArrayList<>();
        int n = text.length(), m = pat.length();
        if (m == 0 || m > n) return res;
        long hp = 0, ht = 0, pow = 1;                 // pow = BASE^(m-1) mod MOD
        for (int i = 0; i < m; i++) {
            hp = (hp * BASE + pat.charAt(i)) % MOD;
            ht = (ht * BASE + text.charAt(i)) % MOD;
            if (i < m - 1) pow = pow * BASE % MOD;
        }
        for (int i = 0; ; i++) {
            if (hp == ht) {                           // possible match -> verify (hash collisions exist)
                int j = 0;
                while (j < m && text.charAt(i + j) == pat.charAt(j)) j++;
                if (j == m) res.add(i);
            }
            if (i + m >= n) break;
            // roll: remove text[i], add text[i+m]
            ht = ((ht - text.charAt(i) * pow % MOD + MOD) * BASE + text.charAt(i + m)) % MOD;
        }
        return res;
    }
}
