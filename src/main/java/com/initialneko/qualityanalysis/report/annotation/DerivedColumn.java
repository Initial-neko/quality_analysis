package com.initialneko.qualityanalysis.report.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a report column whose value is computed by a no-arg public method.
 * This is the extension point for derived columns (rates, summaries, insight
 * labels...) that are not plain entity fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DerivedColumn {
    /** Sort key; columns render in ascending order (ties keep declaration order). */
    int order() default 0;

    /** Display header text. */
    String header() default "";

    /** Column width in characters (used for Excel column width). */
    int width() default 16;

    /** Cell semantics for both renderers. */
    CellType type() default CellType.TEXT;
}
