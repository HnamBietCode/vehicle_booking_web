package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Rating;
import com.bookvehicle.example.sr.model.RatingRefType;
import com.bookvehicle.example.sr.model.RatingTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            RatingTargetType targetType, Long targetId);

    boolean existsByReviewerIdAndTargetTypeAndTargetIdAndRefTypeAndRefId(
            Long reviewerId, RatingTargetType targetType, Long targetId,
            RatingRefType refType, Long refId);

    @Query("SELECT AVG(r.stars) FROM Rating r " +
           "WHERE r.targetType = :targetType AND r.targetId = :targetId")
    Double findAvgStarsByTargetTypeAndTargetId(
            @Param("targetType") RatingTargetType targetType,
            @Param("targetId") Long targetId);

    long countByTargetTypeAndTargetId(RatingTargetType targetType, Long targetId);

    void deleteByReviewerId(Long reviewerId);
}
