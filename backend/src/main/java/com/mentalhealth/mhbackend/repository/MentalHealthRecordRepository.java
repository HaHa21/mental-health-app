package com.mentalhealth.mhbackend.repository;

import com.mentalhealth.mhbackend.model.MentalHealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentalHealthRecordRepository extends JpaRepository<MentalHealthRecord, Long> {

    Optional<MentalHealthRecord> findByDatasetIdAndResourceIdAndYearAndStateAndConditionAndMetric(
            String datasetId, String resourceId, Integer year, String state, String condition, String metric);

    /** Summary grouped by condition and year. */
    @Query("SELECT m.condition, m.year, SUM(m.value) FROM MentalHealthRecord m GROUP BY m.condition, m.year ORDER BY m.year")
    List<Object[]> findConditionYearSummary();

    /** Trend data for a single condition over years. */
    @Query("SELECT m.year, SUM(m.value) FROM MentalHealthRecord m WHERE lower(m.condition) = lower(:condition) GROUP BY m.year ORDER BY m.year")
    List<Object[]> findTrendByCondition(@Param("condition") String condition);

    /** Breakdown by state for a given year. */
    @Query("SELECT m.state, m.condition, SUM(m.value) FROM MentalHealthRecord m WHERE m.year = :year GROUP BY m.state, m.condition ORDER BY m.state")
    List<Object[]> findByYearGroupedByState(@Param("year") Integer year);
}
