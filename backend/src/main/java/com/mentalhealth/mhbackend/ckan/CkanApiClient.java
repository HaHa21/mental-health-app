package com.mentalhealth.mhbackend.ckan;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class CkanApiClient {

    private final RestTemplate restTemplate;

    @Value("${moh.ckan.base-url}")
    private String baseUrl;

    /** Search packages (datasets) matching the given free-text query. */
    public JsonNode packageSearch(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/package_search")
                .queryParam("q", query)
                .queryParam("rows", 100)
                .toUriString();
        log.debug("CKAN package_search: {}", url);
        return restTemplate.getForObject(url, JsonNode.class);
    }

    /** Retrieve metadata for a specific resource. */
    public JsonNode resourceShow(String resourceId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/resource_show")
                .queryParam("id", resourceId)
                .toUriString();
        return restTemplate.getForObject(url, JsonNode.class);
    }

    /** Fetch up to 10 000 records from the CKAN datastore for a resource. */
    public JsonNode datastoreSearch(String resourceId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/datastore_search")
                .queryParam("resource_id", resourceId)
                .queryParam("limit", 10000)
                .toUriString();
        log.debug("CKAN datastore_search: {}", url);
        return restTemplate.getForObject(url, JsonNode.class);
    }
}
