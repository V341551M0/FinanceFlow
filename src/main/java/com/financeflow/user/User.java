package com.financeflow.user;

import com.financeflow.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BRL";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String locale = "pt-BR";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "America/Sao_Paulo";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Theme theme = Theme.DARK;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}