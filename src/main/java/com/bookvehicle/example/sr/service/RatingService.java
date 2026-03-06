package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.RatingForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.RatingRepository;
import com.bookvehicle.example.sr.repository.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final VehicleRepository vehicleRepository;

    public RatingService(RatingRepository ratingRepository,
                         VehicleRepository vehicleRepository) {
        this.ratingRepository = ratingRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // ── Read ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Rating> findDriverRatings(Long driverId) {
        return ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                RatingTargetType.DRIVER, driverId);
    }

    @Transactional(readOnly = true)
    public List<Rating> findVehicleRatings(Long vehicleId) {
        return ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                RatingTargetType.VEHICLE, vehicleId);
    }

    @Transactional(readOnly = true)
    public Double getAvgRating(RatingTargetType targetType, Long targetId) {
        Double avg = ratingRepository.findAvgStarsByTargetTypeAndTargetId(targetType, targetId);
        return avg != null ? avg : 0.0;
    }

    // ── Create ──────────────────────────────────────────────────────

    /**
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public String rate(Long reviewerUserId, RatingForm form) {

        // Validate stars
        if (form.getStars() == null || form.getStars() < 1 || form.getStars() > 5)
            return "Số sao không hợp lệ (phải từ 1 đến 5).";

        RatingTargetType targetType;
        RatingRefType refType;
        try {
            targetType = RatingTargetType.valueOf(form.getTargetType().toUpperCase());
            refType = RatingRefType.valueOf(form.getRefType().toUpperCase());
        } catch (Exception e) {
            return "Loại đánh giá không hợp lệ.";
        }

        // Kiểm tra đã đánh giá chưa
        if (ratingRepository.existsByReviewerIdAndTargetTypeAndTargetIdAndRefTypeAndRefId(
                reviewerUserId, targetType, form.getTargetId(), refType, form.getRefId())) {
            return "Bạn đã đánh giá đối tượng này rồi.";
        }

        Rating r = new Rating();
        r.setReviewerId(reviewerUserId);
        r.setTargetType(targetType);
        r.setTargetId(form.getTargetId());
        r.setRefType(refType);
        r.setRefId(form.getRefId());
        r.setStars(form.getStars());
        r.setComment(form.getComment() != null ? form.getComment().trim() : null);
        try {
            ratingRepository.save(r);
        } catch (DataIntegrityViolationException e) {
            return "Bạn đã đánh giá đối tượng này rồi.";
        }

        // Cập nhật avg_rating cho xe nếu target là VEHICLE
        if (targetType == RatingTargetType.VEHICLE) {
            vehicleRepository.findById(form.getTargetId()).ifPresent(v -> {
                Double avg = ratingRepository.findAvgStarsByTargetTypeAndTargetId(
                        RatingTargetType.VEHICLE, form.getTargetId());
                if (avg != null) {
                    v.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                    vehicleRepository.save(v);
                }
            });
        }

        return null;
    }
}
