package com.initialneko.qualityanalysis.report.annotation;

/** Cell semantics shared by the HTML and Excel renderers. */
public enum CellType {
    /** Plain escaped text. */
    TEXT,
    /** Numeric value; Excel gets a numeric cell, HTML renders the number. */
    NUMBER,
    /** Ratio in 0..1; Excel gets a percent style, HTML renders "12.34%". */
    PERCENT,
    /** Long multi-line text; Excel enables wrap, HTML uses the "values" style. */
    WRAP,
    /** Date/time text; rendered as text (reserved for future formatting). */
    DATE
}
