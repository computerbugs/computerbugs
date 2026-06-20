package com.example.changtest.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "login_time", nullable = false)
    private Date loginTime;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private Boolean success;

    @Column(length = 255)
    private String message;

    @PrePersist
    protected void onCreate() {
        this.loginTime = new Date();
    }
}
