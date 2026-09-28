package com.mentalhealth.mhbackend.ckan;

import com.fasterxml.jackson.databind.JsonNode;
import com.mentalhealth.mhbackend.model.MentalHealthRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps raw CKAN datastore records to {@link MentalHealthRecord} entities.
 *
 * <p>Column name mappings are best-guess defaults based on common MOH dataset patterns.
 * Update the constants below once real data is inspected (see COLUMN_MAPPINGS.md).
 */
@Slf4j
@Component
public class MohDataTransformer {

    // --- Configurable column name mappings ---
    private static final String COL_YEAR      = "year";
    private static final String COL_STATE     = "state";
    private static final String COL_CONDITION = "condition";
    private static final String COL_METRIC    = "metric";
    private static final String COL_VALUE     = "value";
    private static final String COL_UNIT      = "unit";

    public List<MentalHealthRecord> transform(String datasetId, String resourceId,
                                               String sourceUrl, JsonNode datastoreResponse) {
        List<MentalHealthRecord> records = new ArrayList<>();

        JsonNode result = datastoreResponse.path("result");
        if (!result.isObject()) {
            log.warn("Unexpected CKAN datastore response for resource {}", resourceId);
            return records;
        }

        JsonNode rows = result.path("records");
        if (!rows.isArray()) {
            return records;
        }

        Instant now = Instant.now();
        for (JsonNode row : rows) {
            try {
                Integer year      = nullableInt(row, COL_YEAR);
                String  state     = nullableText(row, COL_STATE);
                String  condition = nullableText(row, COL_CONDITION);
                String  metric    = nullableText(row, COL_METRIC);
                BigDecimal value  = nullableDecimal(row, COL_VALUE);

                if (year == null || state == null || condition == null || metric == null) {
                    // Try to derive reasonable fallbacks from what the row contains
                    if (year == null)      year      = deriveYear(row);
                    if (state == null)     state     = deriveState(row);
                    if (condition == null) condition = "Unknown";
                    if (metric == null)    metric    = "count";
                }

                records.add(MentalHealthRecord.builder()
                        .datasetId(datasetId)
                        .resourceId(resourceId)
                        .year(year)
                        .state(state)
                        .condition(condition)
                        .metric(metric)
                        .value(value)
                        .unit(nullableText(row, COL_UNIT))
                        .sourceUrl(sourceUrl)
                        .syncedAt(now)
                        .build());
            } catch (Exception e) {
                log.debug("Skipping malformed CKAN row: {}", e.getMessage());
            }
        }
        return records;
    }

    // --- helpers ---

    private String nullableText(JsonNode row, String field) {
        JsonNode node = row.get(field);
        if (node == null || node.isNull()) return null;
        // try case-insensitive fallback
        if (node.isMissingNode()) {
            for (var it = row.fields(); it.hasNext(); ) {
                var entry = it.next();
                if (entry.getKey().equalsIgnoreCase(field)) return entry.getValue().asText(null);
            }
        }
        return node.asText(null);
    }

    private Integer nullableInt(JsonNode row, String field) {
        String text = nullableText(row, field);
        if (text == null) return null;
        try { return Integer.parseInt(text.trim()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal nullableDecimal(JsonNode row, String field) {
        String text = nullableText(row, field);
        if (text == null) return null;
        try { return new BigDecimal(text.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** Attempt to find a year value from any field whose name contains "year". */
    private Integer deriveYear(JsonNode row) {
        for (var it = row.fields(); it.hasNext(); ) {
            var entry = it.next();
            if (entry.getKey().toLowerCase().contains("year")) {
                try { return Integer.parseInt(entry.getValue().asText().trim()); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Attempt to find a state value from any field whose name contains "state" or "negeri". */
    private String deriveState(JsonNode row) {
        for (var it = row.fields(); it.hasNext(); ) {
            var entry = it.next();
            String key = entry.getKey().toLowerCase();
            if (key.contains("state") || key.contains("negeri")) {
                return entry.getValue().asText(null);
            }
        }
        return "Unknown";
    }
}
