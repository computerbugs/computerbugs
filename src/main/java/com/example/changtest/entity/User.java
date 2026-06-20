package com.example.changtest.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户实体类，映射到 users 表。
 * 存储注册用户的基本信息，包含用户名、加密密码和创建时间。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /** 用户ID，主键自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名，唯一且不能为空，最大50字符 */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /** 加密后的密码，不能为空，最大255字符 */
    @Column(nullable = false, length = 255)
    private String password;

    /** 账号创建时间，自动设置为当前时间 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    /** 实体持久化前自动设置创建时间 */
    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
    }
}
