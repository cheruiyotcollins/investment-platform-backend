package com.investment.backend.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public class ReferredUsers {
   private String name;
   private String status;
   private LocalDateTime date;
   private BigDecimal reward;
}
