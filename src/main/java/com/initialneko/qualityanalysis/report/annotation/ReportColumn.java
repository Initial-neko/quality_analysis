package com.initialneko.qualityanalysis.report.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a report column backed by a public field or a no-arg public method.
 * The same annotation drives both the HTML and Excel renderers, so header text,
 * order, width and formatting stay in one place.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ReportColumn {
    /** Sort key; columns render in ascending order (ties keep declaration order). */
    int order() default 0;

    /** Display header text. */
    String header() default "";

    /** Column width in characters (used for Excel column width). */
    int width() default 16;

    /** Cell semantics for both renderers. */
    CellType type() default CellType.TEXT;
}
