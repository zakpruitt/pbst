package com.zakpruitt.collectingwithzak.ebay;

import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.mapper.SaleMapper;
import com.zakpruitt.collectingwithzak.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EbaySaleUpsertService {

    private final SaleRepository saleRepo;
    private final SaleMapper saleMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertFromEbay(Sale sale) {
        saleRepo.findByEbayOrderId(sale.getEbayOrderId()).ifPresentOrElse(
                existing -> saleMapper.updateFromEbay(sale, existing),
                () -> {
                    sale.setStatus(SaleStatus.STAGED);
                    saleRepo.save(sale);
                });
    }
}
