package com.mentalhealth.mhbackend.service;

import com.mentalhealth.mhbackend.dto.StatsResponse;
import com.mentalhealth.mhbackend.model.MentalHealthRecord;
import com.mentalhealth.mhbackend.repository.MentalHealthRecordRepository;
import com.mentalhealth.mhbackend.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final MentalHealthRecordRepository recordRepository;
    private final SyncLogRepository syncLogRepository;
    private final MohSyncService syncService;

    // ---- Overview: { condition -> { year -> sum } } ----
    public StatsResponse<Map<String, Map<Integer, Double>>> getOverview() {
        List<Object[]> rows = recordRepository.findConditionYearSummary();
        Map<String, Map<Integer, Double>> data = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String condition = (String) row[0];
            Integer year = (Integer) row[1];
            Double value = row[2] instanceof Number n ? n.doubleValue() : 0.0;
            data.computeIfAbsent(condition, k -> new TreeMap<>()).put(year, value);
        }
        return StatsResponse.of(data, resolveSource(), lastSynced());
    }

    // ---- Trend by condition: labels (years) + datasets ----
    public StatsResponse<ChartData> getByCondition(String condition) {
        List<Object[]> rows = recordRepository.findTrendByCondition(condition);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(String.valueOf(row[0]));
            values.add(row[1] instanceof Number n ? n.doubleValue() : 0.0);
        }
        ChartData chart = new ChartData(labels, List.of(new ChartDataset(condition, values)));
        return StatsResponse.of(chart, resolveSource(), lastSynced());
    }

    // ---- Breakdown by state for a given year ----
    public StatsResponse<Map<String, Map<String, Double>>> getByState(Integer year) {
        List<Object[]> rows = recordRepository.findByYearGroupedByState(year);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String state = (String) row[0];
            String condition = (String) row[1];
            Double value = row[2] instanceof Number n ? n.doubleValue() : 0.0;
            data.computeIfAbsent(state, k -> new LinkedHashMap<>()).put(condition, value);
        }
        return StatsResponse.of(data, resolveSource(), lastSynced());
    }

    // ---- Full dataset (professional) ----
    public StatsResponse<Page<MentalHealthRecord>> getFullDataset(Pageable pageable) {
        Page<MentalHealthRecord> page = recordRepository.findAll(pageable);
        return StatsResponse.of(page, resolveSource(), lastSynced());
    }

    // ---- Helpers ----

    private String resolveSource() {
        return syncService.isDataFresh() ? "local" : "live";
    }

    private String lastSynced() {
        return syncLogRepository.findTopByOrderBySyncedAtDesc()
                .map(l -> l.getSyncedAt().toString())
                .orElse(null);
    }

    // ---- Inner chart-shaped DTOs ----

    public record ChartData(List<String> labels, List<ChartDataset> datasets) {}
    public record ChartDataset(String label, List<Double> data) {}
}
