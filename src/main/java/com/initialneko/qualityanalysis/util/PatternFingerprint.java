package com.initialneko.qualityanalysis.util;

public final class PatternFingerprint {
    private PatternFingerprint() {}

    public static String of(String value) {
        if (value == null || value.length() == 0) return "EMPTY";
        StringBuilder out = new StringBuilder();
        char previousClass = 0;
        int run = 0;
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            offset += Character.charCount(cp);
            char currentClass = classify(cp);
            if (isLiteral(currentClass)) {
                flush(out, previousClass, run);
                previousClass = 0;
                run = 0;
                out.append((char) cp);
                continue;
            }
            if (currentClass == previousClass) run++;
            else {
                flush(out, previousClass, run);
                previousClass = currentClass;
                run = 1;
            }
        }
        flush(out, previousClass, run);
        return out.toString();
    }

    private static void flush(StringBuilder out, char clazz, int run) {
        if (clazz == 0 || run <= 0) return;
        out.append(clazz).append(run);
    }

    private static char classify(int cp) {
        if (Character.isDigit(cp)) return 'D';
        if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) return 'C';
        if (Character.isLetter(cp)) return 'L';
        if (Character.isWhitespace(cp)) return 'W';
        if (cp == '-' || cp == '_' || cp == '/' || cp == '.' || cp == ':' || cp == '@' || cp == '#') return (char) cp;
        return 'S';
    }

    private static boolean isLiteral(char c) {
        return c == '-' || c == '_' || c == '/' || c == '.' || c == ':' || c == '@' || c == '#';
    }
}
