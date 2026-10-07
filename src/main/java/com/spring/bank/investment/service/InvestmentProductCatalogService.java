package com.spring.bank.investment.service;

import com.spring.bank.investment.dto.InvestmentProductResponseDTO;
import com.spring.bank.investment.enums.ProductType;
import com.spring.bank.investment.model.InvestmentProduct;
import com.spring.bank.investment.repository.InvestmentProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentProductCatalogService {

    private final InvestmentProductRepository investmentProductRepository;

    public void seedDefaults() {
        if (investmentProductRepository.count() > 0) {
            return;
        }

        List<InvestmentProduct> defaultProducts = List.of(
                buildProduct("CDB 120% CDI", ProductType.CDB, "0.13", "1.20", "500", 0, false),
                buildProduct("CDB 100% CDI", ProductType.CDB, "0.108", "1.00", "100", 0, false),
                buildProduct("LCI 95% CDI", ProductType.LCI, "0.103", "0.95", "1000", 90, true),
                buildProduct("LCA 90% CDI", ProductType.LCA, "0.097", "0.90", "1000", 90, true),
                buildProduct("Tesouro Selic 2027", ProductType.TESOURO_SELIC, "0.108", null, "30", 1, false),
                buildProduct(
                        "Tesouro Prefixado 12.75% 2029",
                        ProductType.TESOURO_PREFIXADO,
                        "0.1275",
                        null,
                        "30",
                        1,
                        false)
        );

        investmentProductRepository.saveAll(defaultProducts);
    }

    private InvestmentProduct buildProduct(
            String name,
            ProductType type,
            String annualRate,
            String cdiPercentage,
            String minimumAmount,
            int minimumDaysToRedeem,
            boolean irExempt) {
        InvestmentProduct product = new InvestmentProduct();
        product.setName(name);
        product.setType(type);
        product.setAnnualRate(new BigDecimal(annualRate));
        product.setCdiPercentage(cdiPercentage != null ? new BigDecimal(cdiPercentage) : null);
        product.setMinimumAmount(new BigDecimal(minimumAmount));
        product.setMinimumDaysToRedeem(minimumDaysToRedeem);
        product.setIrExempt(irExempt);
        product.setActive(true);
        return product;
    }

    public List<InvestmentProductResponseDTO> listAll() {
        return investmentProductRepository.findByActiveTrue().stream()
                .map(InvestmentProductResponseDTO::new)
                .toList();
    }

    public InvestmentProduct getById(Long id) {
        return investmentProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }
}
