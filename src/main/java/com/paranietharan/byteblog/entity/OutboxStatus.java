package com.paranietharan.byteblog.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
