package com.mentalhealth.mhbackend.ckan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Fetches mental-health-adjacent datasets from the Malaysian open data portal
 * (storage.data.gov.my).
 *
 * <p>The original plan referenced the CKAN API at data.moh.gov.my, but that site
 * is a NextJS frontend with no CKAN endpoint. The real programmatic access is via
 * pre-built CSV/Parquet files on {@code https://storage.data.gov.my}.</p>
 *
 * <p>Known datasets (all under the Public Safety category on data.gov.my):</p>
 * <ul>
 *   <li>{@code drug_addicts_age} — yearly addicts count by state &amp; age group</li>
 *   <li>{@code drug_addicts_drugtype} — yearly addicts count by state &amp; drug type</li>
 *   <li>{@code drug_addicts_education} — yearly addicts count by state &amp; education</li>
 *   <li>{@code drug_addicts_occupation} — yearly addicts count by state &amp; occupation</li>
 *   <li>{@code drug_arrests_age} — yearly drug arrests by sex &amp; age</li>
 * </ul>
 */
@Slf4j
@Component
public class MohDataApiClient {

    /** Dataset descriptor: logical ID, storage sub-folder, and filename stem. */
    public record DatasetDescriptor(String id, String folder, String fileStem) {
        public String csvUrl(String storageBaseUrl) {
            return storageBaseUrl + "/" + folder + "/" + fileStem + ".csv";
        }
    }

    /** All datasets that feed mental-health-related stats. */
    public static final List<DatasetDescriptor> MENTAL_HEALTH_DATASETS = List.of(
            new DatasetDescriptor("drug_addicts_age",        "publicsafety", "drug_addicts_age"),
            new DatasetDescriptor("drug_addicts_drugtype",   "publicsafety", "drug_addicts_drugtype"),
            new DatasetDescriptor("drug_addicts_education",  "publicsafety", "drug_addicts_education"),
            new DatasetDescriptor("drug_addicts_occupation", "publicsafety", "drug_addicts_occupation"),
            new DatasetDescriptor("drug_arrests_age",        "publicsafety", "drug_arrests_age")
    );

    private final RestTemplate restTemplate;

    @Value("${moh.data.storage-base-url}")
    private String storageBaseUrl;

    public MohDataApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Downloads the CSV content for the given dataset descriptor.
     *
     * @return raw CSV string, or {@code null} if the request fails
     */
    public String fetchCsv(DatasetDescriptor descriptor) {
        String url = descriptor.csvUrl(storageBaseUrl);
        log.debug("Fetching MOH dataset: {}", url);
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.warn("Failed to fetch dataset '{}' from {}: {}", descriptor.id(), url, e.getMessage());
            return null;
        }
    }
}
