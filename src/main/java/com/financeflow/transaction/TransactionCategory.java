package com.financeflow.transaction;

import com.financeflow.common.BaseEntity;
import com.financeflow.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 7)  private String color;
    @Column(length = 40) private String icon;
}