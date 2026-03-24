package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.Customer;
import com.bookvehicle.example.sr.model.MembershipType;
import com.bookvehicle.example.sr.model.NotificationRefType;
import com.bookvehicle.example.sr.model.NotificationType;
import com.bookvehicle.example.sr.model.PremiumTier;
import com.bookvehicle.example.sr.model.ReferenceType;
import com.bookvehicle.example.sr.model.TransactionType;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PremiumService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CustomerRepository customerRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    public PremiumService(CustomerRepository customerRepository,
                          WalletService walletService,
                          NotificationService notificationService) {
        this.customerRepository = customerRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

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

    @Transactional(readOnly = true)
    public List<PremiumTier> getAvailableTiers() {
        return Arrays.asList(PremiumTier.values());
    }

    @Transactional(readOnly = true)
    public PremiumTier parseTier(String rawTier) {
        if (rawTier == null || rawTier.isBlank()) {
            return null;
        }
        try {
            return PremiumTier.valueOf(rawTier.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public PurchaseResult purchase(Long userId, PremiumTier tier) {
        if (tier == null) {
            return PurchaseResult.error("Goi premium khong hop le.");
        }

        Optional<Customer> opt = customerRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return PurchaseResult.error("Khong tim thay ho so khach hang.");
        }

        Customer customer = opt.get();
        String debitError = walletService.debit(
                userId,
                tier.getPrice(),
                TransactionType.PREMIUM_UPGRADE,
                ReferenceType.MANUAL,
                customer.getId(),
                "Thanh toan goi Premium hang " + tier.getDisplayName()
        );
        if (debitError != null) {
            return PurchaseResult.error(debitError);
        }

        LocalDate baseDate = customer.isPremiumActive() && customer.getPremiumExp() != null
                ? customer.getPremiumExp()
                : LocalDate.now();
        LocalDate expiry = baseDate.plusMonths(1);

        customer.setMembership(MembershipType.PREMIUM);
        customer.setPremiumTier(tier);
        customer.setPremiumExp(expiry);
        customerRepository.save(customer);

        notificationService.createNotification(
                userId,
                "Nang cap premium thanh cong",
                "Ban da kich hoat hang " + tier.getDisplayName() + " den ngay " + expiry.format(DATE_FORMATTER) + ".",
                NotificationType.GENERAL,
                NotificationRefType.SYSTEM,
                customer.getId()
        );

        return PurchaseResult.success(
                "Da kich hoat goi hang " + tier.getDisplayName() + " den " + expiry.format(DATE_FORMATTER) + "."
        );
    }

    public String cancel(Long userId) {
        Optional<Customer> opt = customerRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return "Khong tim thay ho so khach hang.";
        }

        Customer customer = opt.get();
        customer.setMembership(MembershipType.STANDARD);
        customer.setPremiumTier(null);
        customer.setPremiumExp(null);
        customerRepository.save(customer);
        return null;
    }

    public String adminSetMembership(Long customerId, String membership, LocalDate expiry) {
        Optional<Customer> opt = customerRepository.findById(customerId);
        if (opt.isEmpty()) {
            return "Khong tim thay khach hang.";
        }

        Customer customer = opt.get();
        try {
            customer.setMembership(MembershipType.valueOf(membership.toUpperCase()));
        } catch (Exception e) {
            return "Loai membership khong hop le.";
        }

        if (customer.getMembership() == MembershipType.STANDARD) {
            customer.setPremiumTier(null);
            customer.setPremiumExp(null);
        } else {
            if (customer.getPremiumTier() == null) {
                customer.setPremiumTier(PremiumTier.BRONZE);
            }
            customer.setPremiumExp(expiry);
        }
        customerRepository.save(customer);
        return null;
    }

    public record PurchaseResult(boolean ok, String message) {
        public static PurchaseResult success(String message) {
            return new PurchaseResult(true, message);
        }

        public static PurchaseResult error(String message) {
            return new PurchaseResult(false, message);
        }
    }
}
