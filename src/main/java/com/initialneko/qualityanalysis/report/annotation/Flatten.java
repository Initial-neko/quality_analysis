package com.initialneko.qualityanalysis.report.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flattens an embedded object: the annotated field's own type is scanned for
 * {@link ReportColumn} fields and {@link DerivedColumn}/{@code @ReportColumn}
 * methods, and those columns join the outer table as if declared inline.
 * Only one nesting level is flattened (the inner scan does not follow further
 * {@code @Flatten} fields).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Flatten {
    /** Optional prefix prepended to each flattened column header. */
    String prefix() default "";
}
