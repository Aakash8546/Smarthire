
package com.smarthire.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RankedCandidate {
    private Long userId;
    private String name;
    private String email;
    private BigDecimal score;
    private String explanation;
    private Integer rank;
}