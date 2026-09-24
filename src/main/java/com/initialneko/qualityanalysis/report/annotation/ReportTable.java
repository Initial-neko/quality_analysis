package com.initialneko.qualityanalysis.report.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level metadata for a reportable entity (title and Excel sheet name). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReportTable {
    /** Human readable report title. */
    String title() default "";

    /** Excel sheet name; defaults to the class simple name. */
    String sheet() default "";
}
