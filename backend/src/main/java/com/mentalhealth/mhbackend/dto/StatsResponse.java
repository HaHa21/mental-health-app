package com.mentalhealth.mhbackend.dto;

import java.util.List;

/**
 * Generic API envelope used by all /api/stats responses.
 *
 * @param data The payload (list of items or any object)
 * @param meta Metadata about the response (source, timing, etc.)
 */
public record StatsResponse<T>(T data, Meta meta) {

    public record Meta(String source, String lastSynced) {}

    public static <T> StatsResponse<T> of(T data, String source, String lastSynced) {
        return new StatsResponse<>(data, new Meta(source, lastSynced));
    }
}
