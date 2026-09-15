package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.ValueFamily;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

/** Inclusive numeric/date range validation. Date bounds use ISO yyyy-MM-dd. */
public final class RangeRule extends AbstractColumnRule {
    private final String minInclusive;
    private final String maxInclusive;
    private Mode mode;
    private BigDecimal minNumber;
    private BigDecimal maxNumber;
    private LocalDate minDate;
    private LocalDate maxDate;

    public RangeRule(String id, String minInclusive, String maxInclusive) {
        super(id);
        if ((minInclusive == null || minInclusive.trim().length() == 0)
                && (maxInclusive == null || maxInclusive.trim().length() == 0)) {
            throw new IllegalArgumentException("at least one range bound is required");
        }
        this.minInclusive = blankToNull(minInclusive);
        this.maxInclusive = blankToNull(maxInclusive);
    }

    @Override
    protected void onStart() {
        if (metadata.getFamily() == ValueFamily.NUMBER) {
            mode = Mode.NUMBER;
            if (minInclusive != null) minNumber = new BigDecimal(minInclusive);
            if (maxInclusive != null) maxNumber = new BigDecimal(maxInclusive);
            if (minNumber != null && maxNumber != null && minNumber.compareTo(maxNumber) > 0) {
                throw new IllegalArgumentException("min range is greater than max range");
            }
            return;
        }
        if (metadata.getFamily() == ValueFamily.DATE_TIME) {
            mode = Mode.DATE;
            if (minInclusive != null) minDate = LocalDate.parse(minInclusive);
            if (maxInclusive != null) maxDate = LocalDate.parse(maxInclusive);
            if (minDate != null && maxDate != null && minDate.isAfter(maxDate)) {
                throw new IllegalArgumentException("min date is after max date");
            }
            return;
        }
        throw new IllegalArgumentException("RangeRule supports NUMBER or DATE_TIME columns, got " + metadata.getFamily());
    }

    @Override
    public void accept(Object value) {
        if (isMissing(value)) return;
        checked();
        try {
            if (mode == Mode.NUMBER) {
                BigDecimal number = toNumber(value);
                if ((minNumber != null && number.compareTo(minNumber) < 0)
                        || (maxNumber != null && number.compareTo(maxNumber) > 0)) invalid(value);
            } else {
                LocalDate date = toDate(value);
                if ((minDate != null && date.isBefore(minDate))
                        || (maxDate != null && date.isAfter(maxDate))) invalid(value);
            }
        } catch (RuntimeException conversionFailure) {
            invalid(value);
        }
    }

    @Override
    public RuleResult finish(ColumnProfile profile) {
        return result(invalidCount == 0L,
                "inclusiveRange=[" + (minInclusive == null ? "-inf" : minInclusive)
                        + ", " + (maxInclusive == null ? "+inf" : maxInclusive) + "]");
    }

    private BigDecimal toNumber(Object value) {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return new BigDecimal(canonical(value));
    }

    private LocalDate toDate(Object value) {
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime().toLocalDate();
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).toLocalDate();
        if (value instanceof OffsetDateTime) return ((OffsetDateTime) value).toLocalDate();
        if (value instanceof Date) return new java.sql.Date(((Date) value).getTime()).toLocalDate();
        String text = canonical(value);
        if (text.length() >= 10) return LocalDate.parse(text.substring(0, 10));
        return LocalDate.parse(text);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private enum Mode { NUMBER, DATE }
}
