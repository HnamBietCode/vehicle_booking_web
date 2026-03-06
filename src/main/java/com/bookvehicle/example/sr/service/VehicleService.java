package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.VehicleForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    // ── Read ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findByCategory(VehicleCategory category) {
        return vehicleRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Driver> findApprovedDriversWithoutVehicle() {
        // Tài xế APPROVED + chưa có xe gán
        return driverRepository.findAll().stream()
                .filter(d -> d.getVerificationStatus() == VerificationStatus.APPROVED)
                .filter(d -> vehicleRepository.findByAssignedDriver(d.getId()).isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Driver> findApprovedDrivers() {
        return driverRepository.findAll().stream()
                .filter(d -> d.getVerificationStatus() == VerificationStatus.APPROVED)
                .toList();
    }

    // ── Search (public) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Vehicle> searchAvailable(String categoryStr, BigDecimal maxPricePerKm,
                                          String location, boolean requireFreeDriver) {
        VehicleCategory category = null;
        if (categoryStr != null && !categoryStr.isBlank()) {
            try { category = VehicleCategory.valueOf(categoryStr.toUpperCase()); }
            catch (Exception ignored) {}
        }
        if (requireFreeDriver) {
            return vehicleRepository.findAvailableWithReadyDriver(category, maxPricePerKm, location);
        }
        return vehicleRepository.findAvailable(category, maxPricePerKm, location);
    }

    // ── Create ──────────────────────────────────────────────────────

    /**
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String create(VehicleForm form) {
        String err = validate(form, null);
        if (err != null) return err;

        Vehicle v = new Vehicle();
        fillFrom(v, form);
        vehicleRepository.save(v);
        return null;
    }

    // ── Update ──────────────────────────────────────────────────────

    public String update(Long id, VehicleForm form) {
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isEmpty()) return "Không tìm thấy xe.";
        String err = validate(form, id);
        if (err != null) return err;
        fillFrom(opt.get(), form);
        vehicleRepository.save(opt.get());
        return null;
    }

    // ── Delete ──────────────────────────────────────────────────────

    public void delete(Long id) {
        vehicleRepository.deleteById(id);
    }

    // ── Assign / Unassign Driver ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Vehicle> findByAssignedDriver(Long driverId) {
        return vehicleRepository.findByAssignedDriver(driverId);
    }

    public String assignDriver(Long vehicleId, Long driverId) {
        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        if (vOpt.isEmpty()) return "Không tìm thấy xe.";
        Optional<Driver> dOpt = driverRepository.findById(driverId);
        if (dOpt.isEmpty()) return "Không tìm thấy tài xế.";
        if (dOpt.get().getVerificationStatus() != VerificationStatus.APPROVED)
            return "Chỉ tài xế đã được duyệt mới có thể gán xe.";

        // Bỏ gán xe cũ của tài xế này nếu có
        vehicleRepository.findByAssignedDriver(driverId).ifPresent(old -> {
            old.setAssignedDriver(null);
            vehicleRepository.save(old);
        });

        Vehicle v = vOpt.get();
        v.setAssignedDriver(driverId);
        vehicleRepository.save(v);
        return null;
    }

    public String unassignDriver(Long vehicleId) {
        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        if (vOpt.isEmpty()) return "Không tìm thấy xe.";
        vOpt.get().setAssignedDriver(null);
        vehicleRepository.save(vOpt.get());
        return null;
    }

    public void updateLocation(Long vehicleId, String location) {
        vehicleRepository.findById(vehicleId).ifPresent(v -> {
            v.setCurrentAddress(location);
            vehicleRepository.save(v);
        });
    }

    // ── Internal helpers ────────────────────────────────────────────

    private String validate(VehicleForm form, Long excludeId) {
        if (form.getName() == null || form.getName().isBlank())
            return "Tên xe không được để trống.";
        if (form.getLicensePlate() == null || form.getLicensePlate().isBlank())
            return "Biển số xe không được để trống.";
        if (excludeId == null && vehicleRepository.existsByLicensePlate(form.getLicensePlate().trim()))
            return "Biển số xe đã tồn tại.";
        if (excludeId != null && vehicleRepository.existsByLicensePlateAndIdNot(
                form.getLicensePlate().trim(), excludeId))
            return "Biển số xe đã tồn tại.";
        if (form.getPricePerKm() == null || form.getPricePerKm().compareTo(BigDecimal.ZERO) <= 0)
            return "Giá/km phải lớn hơn 0.";
        if (form.getPricePerHour() == null || form.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0)
            return "Giá/giờ phải lớn hơn 0.";
        if (form.getPricePerDay() == null || form.getPricePerDay().compareTo(BigDecimal.ZERO) <= 0)
            return "Giá/ngày phải lớn hơn 0.";
        try { VehicleCategory.valueOf(form.getCategory().toUpperCase()); }
        catch (Exception e) { return "Loại xe không hợp lệ."; }
        return null;
    }

    private void fillFrom(Vehicle v, VehicleForm form) {
        v.setCategory(VehicleCategory.valueOf(form.getCategory().toUpperCase()));
        v.setName(form.getName().trim());
        v.setLicensePlate(form.getLicensePlate().trim().toUpperCase());
        v.setColor(form.getColor() != null ? form.getColor().trim() : null);
        v.setYear(form.getYear());
        v.setImageUrl(form.getImageUrl() != null && !form.getImageUrl().isBlank()
                ? form.getImageUrl().trim() : null);
        v.setPricePerKm(form.getPricePerKm());
        v.setPricePerHour(form.getPricePerHour());
        v.setPricePerDay(form.getPricePerDay());
        v.setCurrentAddress(form.getCurrentAddress() != null ? form.getCurrentAddress().trim() : null);
        if (form.getStatus() != null && !form.getStatus().isBlank()) {
            try { v.setStatus(VehicleStatus.valueOf(form.getStatus().toUpperCase())); }
            catch (Exception ignored) {}
        }
        // assignedDriver được xử lý qua assignDriver() riêng
    }
}
