package com.example.changtest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Component
public class DatabaseMigration {

    @Autowired
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fixCreatedAtColumn() {
        try {
            // created_at 列在数据库中为 bytea 类型（存储了 Java 序列化对象），无法直接 ALTER TYPE
            // 需要先删除再重建为 TIMESTAMP
            entityManager.createNativeQuery(
                "ALTER TABLE users DROP COLUMN IF EXISTS created_at"
            ).executeUpdate();
            entityManager.createNativeQuery(
                "ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW()"
            ).executeUpdate();
            System.out.println(">>> created_at column recreated as TIMESTAMP");
        } catch (Exception e) {
            System.out.println(">>> created_at migration: " + e.getMessage());
        }
    }
}
