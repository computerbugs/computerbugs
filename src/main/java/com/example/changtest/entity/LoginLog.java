package com.example.changtest.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * 登录日志实体类，映射到 login_logs 表。
 * 记录每一次登录操作行为，包括用户名、IP地址、登录时间和成功/失败状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "login_logs")
public class LoginLog {

    /** 日志ID，主键自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录时输入的用户名 */
    @Column(nullable = false, length = 50)
    private String username;

    /** 登录时间，自动设置为当前时间 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "login_time", nullable = false)
    private Date loginTime;

    /** 客户端IP地址，支持IPv4和IPv6，最大45字符 */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 登录是否成功，true-成功，false-失败 */
    @Column(nullable = false)
    private Boolean success;

    /** 附加信息，如成功提示或失败原因 */
    @Column(length = 255)
    private String message;

    /** 实体持久化前自动设置登录时间 */
    @PrePersist
    protected void onCreate() {
        this.loginTime = new Date();
    }
}
