package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.Customer;
import com.bookvehicle.example.sr.model.MembershipType;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PremiumService {

    private final CustomerRepository customerRepository;

    public PremiumService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // ── Read ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findByUserId(Long userId) {
        return customerRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    // ── Upgrade ───────────────────────────────────────────────────
    /**
     * Nâng cấp membership lên PREMIUM.
     * @param userId  ID user (not customer.id)
     * @param months  Số tháng Premium (1, 3, 6, 12…)
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String upgrade(Long userId, int months) {
        if (months <= 0) return "Số tháng không hợp lệ.";
        Optional<Customer> opt = customerRepository.findByUserId(userId);
        if (opt.isEmpty()) return "Không tìm thấy hồ sơ khách hàng.";
        Customer c = opt.get();

        LocalDate expiry;
        if (c.getMembership() == MembershipType.PREMIUM && c.getPremiumExp() != null
                && c.getPremiumExp().isAfter(LocalDate.now())) {
            // Cộng thêm vào ngày hết hạn hiện tại
            expiry = c.getPremiumExp().plusMonths(months);
        } else {
            expiry = LocalDate.now().plusMonths(months);
        }

        c.setMembership(MembershipType.PREMIUM);
        c.setPremiumExp(expiry);
        customerRepository.save(c);
        return null;
    }

    // ── Cancel / Downgrade ────────────────────────────────────────
    /**
     * Huỷ Premium – chuyển về STANDARD.
     */
    public String cancel(Long userId) {
        Optional<Customer> opt = customerRepository.findByUserId(userId);
        if (opt.isEmpty()) return "Không tìm thấy hồ sơ khách hàng.";
        Customer c = opt.get();
        c.setMembership(MembershipType.STANDARD);
        c.setPremiumExp(null);
        customerRepository.save(c);
        return null;
    }

    // ── Admin: set trực tiếp ──────────────────────────────────────
    public String adminSetMembership(Long customerId, String membership, LocalDate expiry) {
        Optional<Customer> opt = customerRepository.findById(customerId);
        if (opt.isEmpty()) return "Không tìm thấy khách hàng.";
        Customer c = opt.get();
        try {
            c.setMembership(MembershipType.valueOf(membership.toUpperCase()));
        } catch (Exception e) {
            return "Loại membership không hợp lệ.";
        }
        c.setPremiumExp(expiry);
        customerRepository.save(c);
        return null;
    }
}
