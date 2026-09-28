package com.mentalhealth.mhbackend.controller;

import com.mentalhealth.mhbackend.dto.StatsResponse;
import com.mentalhealth.mhbackend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** Public — summary by condition and year. */
    @GetMapping("/overview")
    public ResponseEntity<StatsResponse<?>> overview() {
        return ResponseEntity.ok(statsService.getOverview());
    }

    /** Public — trend data for a single condition. */
    @GetMapping("/by-condition")
    public ResponseEntity<StatsResponse<?>> byCondition(
            @RequestParam(defaultValue = "Depression") String condition) {
        return ResponseEntity.ok(statsService.getByCondition(condition));
    }

    /** Public — state breakdown for a year. */
    @GetMapping("/by-state")
    public ResponseEntity<StatsResponse<?>> byState(
            @RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : java.time.Year.now().getValue();
        return ResponseEntity.ok(statsService.getByState(resolvedYear));
    }

    /** Professional only — full paginated dataset. */
    @GetMapping("/advanced/full")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<StatsResponse<?>> advancedFull(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("year").descending());
        return ResponseEntity.ok(statsService.getFullDataset(pageable));
    }
}
