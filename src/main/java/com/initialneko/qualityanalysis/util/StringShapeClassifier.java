package com.initialneko.qualityanalysis.util;

public final class StringShapeClassifier {
    private StringShapeClassifier() {}

    public static Shape classify(String value) {
        boolean digit = false;
        boolean letter = false;
        boolean han = false;
        boolean whitespace = false;
        boolean special = false;
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            offset += Character.charCount(cp);
            if (Character.isDigit(cp)) digit = true;
            else if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) han = true;
            else if (Character.isLetter(cp)) letter = true;
            else if (Character.isWhitespace(cp)) whitespace = true;
            else special = true;
        }
        return new Shape(digit, letter, han, whitespace, special);
    }

    public static final class Shape {
        public final boolean digit;
        public final boolean letter;
        public final boolean han;
        public final boolean whitespace;
        public final boolean special;

        Shape(boolean digit, boolean letter, boolean han, boolean whitespace, boolean special) {
            this.digit = digit;
            this.letter = letter;
            this.han = han;
            this.whitespace = whitespace;
            this.special = special;
        }

        public boolean numericOnly() { return digit && !letter && !han && !whitespace && !special; }
        public boolean alphabeticOnly() { return letter && !digit && !han && !whitespace && !special; }
        public boolean alphanumericOnly() { return digit && letter && !han && !whitespace && !special; }
        public boolean chineseOnly() { return han && !digit && !letter && !whitespace && !special; }
        public boolean mixed() {
            int classes = (digit ? 1 : 0) + (letter ? 1 : 0) + (han ? 1 : 0) + (whitespace ? 1 : 0) + (special ? 1 : 0);
            return classes > 1;
        }
    }
}
