package com.collectingwithzak.service;

import com.collectingwithzak.dto.sale.RangeTotals;
import com.collectingwithzak.dto.vince.CreateVincePaymentRequest;
import com.collectingwithzak.dto.vince.VinceLedger;
import com.collectingwithzak.dto.vince.VincePaymentResponse;
import com.collectingwithzak.dto.vince.VincePaymentTotals;
import com.collectingwithzak.mapper.VincePaymentMapper;
import com.collectingwithzak.repository.SaleRepository;
import com.collectingwithzak.repository.VincePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VincePaymentService {

    private final VincePaymentRepository paymentRepo;
    private final SaleRepository saleRepo;
    private final VincePaymentMapper paymentMapper;

    public void create(CreateVincePaymentRequest request) {
        paymentRepo.save(paymentMapper.toEntity(request));
    }

    public List<VincePaymentResponse> getAll() {
        return paymentMapper.toResponseList(paymentRepo.findAllByOrderByPaymentDateDescIdDesc());
    }

    public VinceLedger getLedger() {
        RangeTotals salesTotals = saleRepo.getVinceTotals();
        VincePaymentTotals totals = paymentRepo.getTotals();
        return VinceLedger.from(salesTotals, totals.getPaidOut(), totals.getVinceOwes());
    }

    public void delete(Long id) {
        paymentRepo.deleteById(id);
    }
}
