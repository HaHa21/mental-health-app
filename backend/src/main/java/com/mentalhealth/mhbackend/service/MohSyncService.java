package com.mentalhealth.mhbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mentalhealth.mhbackend.ckan.CkanApiClient;
import com.mentalhealth.mhbackend.ckan.MohDataTransformer;
import com.mentalhealth.mhbackend.model.MentalHealthRecord;
import com.mentalhealth.mhbackend.model.SyncLog;
import com.mentalhealth.mhbackend.model.SyncStatus;
import com.mentalhealth.mhbackend.repository.MentalHealthRecordRepository;
import com.mentalhealth.mhbackend.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MohSyncService {

    private final CkanApiClient ckanApiClient;
    private final MohDataTransformer transformer;
    private final MentalHealthRecordRepository recordRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${moh.sync.freshness-threshold-hours:24}")
    private long freshnessThresholdHours;

    private static final List<String> SEARCH_QUERIES =
            List.of("mental health", "depression", "anxiety");

    @Scheduled(cron = "${moh.sync.cron:0 0 2 * * *}")
    public void scheduledSync() {
        log.info("Scheduled MOH sync starting");
        sync();
    }

    @Transactional
    public SyncLog sync() {
        Instant start = Instant.now();
        int totalRecords = 0;
        try {
            // Discover resources — de-duplicate by resourceId
            Map<String, String[]> resources = discoverResources(); // resourceId -> [datasetId, sourceUrl]

            for (Map.Entry<String, String[]> entry : resources.entrySet()) {
                String resourceId = entry.getKey();
                String datasetId  = entry.getValue()[0];
                String sourceUrl  = entry.getValue()[1];
                try {
                    JsonNode datastoreResponse = ckanApiClient.datastoreSearch(resourceId);
                    List<MentalHealthRecord> records =
                            transformer.transform(datasetId, resourceId, sourceUrl, datastoreResponse);
                    for (MentalHealthRecord record : records) {
                        upsert(record);
                        totalRecords++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to sync resource {}: {}", resourceId, e.getMessage());
                }
            }

            SyncLog syncLog = SyncLog.builder()
                    .syncedAt(start)
                    .status(SyncStatus.SUCCESS)
                    .recordsFetched(totalRecords)
                    .build();
            return syncLogRepository.save(syncLog);

        } catch (Exception e) {
            log.error("MOH sync failed: {}", e.getMessage(), e);
            SyncLog syncLog = SyncLog.builder()
                    .syncedAt(start)
                    .status(SyncStatus.FAIL)
                    .recordsFetched(totalRecords)
                    .errorMessage(e.getMessage())
                    .build();
            return syncLogRepository.save(syncLog);
        }
    }

    public boolean isDataFresh() {
        return syncLogRepository.findTopByOrderBySyncedAtDesc()
                .map(log -> log.getSyncedAt()
                        .isAfter(Instant.now().minus(freshnessThresholdHours, ChronoUnit.HOURS)))
                .orElse(false);
    }

    // --- private helpers ---

    private Map<String, String[]> discoverResources() {
        Map<String, String[]> resources = new LinkedHashMap<>();
        for (String query : SEARCH_QUERIES) {
            try {
                JsonNode response = ckanApiClient.packageSearch(query);
                JsonNode packages = response.path("result").path("results");
                if (packages.isArray()) {
                    for (JsonNode pkg : packages) {
                        String datasetId = pkg.path("id").asText();
                        String sourceUrl = pkg.path("url").asText(null);
                        JsonNode resArray = pkg.path("resources");
                        if (resArray.isArray()) {
                            for (JsonNode res : resArray) {
                                String resId = res.path("id").asText();
                                if (!resId.isEmpty()) {
                                    resources.putIfAbsent(resId, new String[]{datasetId, sourceUrl});
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("CKAN package_search failed for query '{}': {}", query, e.getMessage());
            }
        }
        return resources;
    }

    private void upsert(MentalHealthRecord incoming) {
        recordRepository.findByDatasetIdAndResourceIdAndYearAndStateAndConditionAndMetric(
                incoming.getDatasetId(), incoming.getResourceId(),
                incoming.getYear(), incoming.getState(),
                incoming.getCondition(), incoming.getMetric())
            .ifPresentOrElse(existing -> {
                existing.setValue(incoming.getValue());
                existing.setUnit(incoming.getUnit());
                existing.setSyncedAt(incoming.getSyncedAt());
                recordRepository.save(existing);
            }, () -> recordRepository.save(incoming));
    }
}
