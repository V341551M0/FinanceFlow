package com.financeflow.investment;

import com.financeflow.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asset extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetType type;

    @Column(length = 20)
    private String exchange;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BRL";
}