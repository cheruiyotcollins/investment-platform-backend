package com.investment.backend.dto;


import lombok.*;

        import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvestRequest {
    private Integer planId;
    private BigDecimal amount; // sent as number
    private String asset;
}