package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.PickupPointForm;
import com.bookvehicle.example.sr.model.PickupPoint;
import com.bookvehicle.example.sr.repository.PickupPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PickupPointService {

    private final PickupPointRepository pickupPointRepository;

    public PickupPointService(PickupPointRepository pickupPointRepository) {
        this.pickupPointRepository = pickupPointRepository;
    }

    @Transactional(readOnly = true)
    public List<PickupPoint> findAll() {
        return pickupPointRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PickupPoint> findById(Long id) {
        return pickupPointRepository.findById(id);
    }

    public String create(PickupPointForm form) {
        String err = validate(form, null);
        if (err != null) {
            return err;
        }
        PickupPoint p = new PickupPoint();
        fillFromForm(p, form);
        pickupPointRepository.save(p);
        return null;
    }

    public String update(Long id, PickupPointForm form) {
        Optional<PickupPoint> opt = pickupPointRepository.findById(id);
        if (opt.isEmpty()) {
            return "Khong tim thay diem don.";
        }
        String err = validate(form, id);
        if (err != null) {
            return err;
        }
        PickupPoint p = opt.get();
        fillFromForm(p, form);
        pickupPointRepository.save(p);
        return null;
    }

    public String deactivate(Long id) {
        Optional<PickupPoint> opt = pickupPointRepository.findById(id);
        if (opt.isEmpty()) {
            return "Khong tim thay diem don.";
        }
        PickupPoint p = opt.get();
        p.setIsActive(false);
        pickupPointRepository.save(p);
        return null;
    }

    public String activate(Long id) {
        Optional<PickupPoint> opt = pickupPointRepository.findById(id);
        if (opt.isEmpty()) {
            return "Khong tim thay diem don.";
        }
        PickupPoint p = opt.get();
        p.setIsActive(true);
        pickupPointRepository.save(p);
        return null;
    }

    private String validate(PickupPointForm form, Long excludeId) {
        if (form.getName() == null || form.getName().isBlank()) {
            return "Ten diem don khong duoc de trong.";
        }
        if (form.getAddress() == null || form.getAddress().isBlank()) {
            return "Dia chi diem don khong duoc de trong.";
        }
        String name = form.getName().trim();
        String address = form.getAddress().trim();
        boolean duplicated = (excludeId == null)
                ? pickupPointRepository.existsByNameIgnoreCaseAndAddressIgnoreCase(name, address)
                : pickupPointRepository.existsByNameIgnoreCaseAndAddressIgnoreCaseAndIdNot(name, address, excludeId);
        if (duplicated) {
            return "Diem don nay da ton tai.";
        }
        return null;
    }

    private void fillFromForm(PickupPoint p, PickupPointForm form) {
        p.setName(form.getName().trim());
        p.setAddress(form.getAddress().trim());
        p.setLatitude(form.getLatitude());
        p.setLongitude(form.getLongitude());
        p.setIsActive(form.getIsActive() == null || form.getIsActive());
    }
}
