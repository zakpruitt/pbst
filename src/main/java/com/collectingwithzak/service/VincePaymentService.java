package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.collectingwithzak.dto.response.RangeTotals;
import com.collectingwithzak.dto.response.VinceLedger;
import com.collectingwithzak.dto.response.VincePaymentResponse;
import com.collectingwithzak.entity.VincePayment;
import com.collectingwithzak.entity.enums.PaymentType;
import com.collectingwithzak.mapper.VincePaymentMapper;
import com.collectingwithzak.repository.SaleRepository;
import com.collectingwithzak.repository.VincePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VincePaymentService {

    private final VincePaymentRepository paymentRepo;
    private final SaleRepository saleRepo;
    private final VincePaymentMapper paymentMapper;

    public void create(CreateVincePaymentRequest request) {
        VincePayment payment = paymentMapper.toEntity(request);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        if (payment.getType() == null || payment.getType().isBlank()) {
            payment.setType(PaymentType.PAYOUT.name());
        }
        paymentRepo.save(payment);
    }

    @Transactional(readOnly = true)
    public List<VincePaymentResponse> getAll() {
        return paymentMapper.toResponseList(paymentRepo.findAllByOrderByPaymentDateDescIdDesc());
    }

    @Transactional(readOnly = true)
    public VinceLedger getLedger() {
        RangeTotals salesTotals = saleRepo.getVinceTotals();
        double[] paymentTotals = paymentRepo.getTotals();
        return VinceLedger.from(salesTotals, paymentTotals[0], paymentTotals[1]);
    }

    public void delete(Long id) {
        paymentRepo.deleteById(id);
    }
}
