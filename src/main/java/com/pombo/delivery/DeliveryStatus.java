package com.pombo.delivery;

public enum DeliveryStatus {
    PENDING,      // aceita, aguardando consumidor
    IN_PROGRESS,  // consumidor assumiu
    RETRYING,     // falha temporária, aguardando nova tentativa
    DELIVERED,    // 2xx
    FAILED,       // falha permanente (4xx), sem retentativa
    DEAD;         // tentativas esgotadas (DLT)

    public boolean isRedeliverable() {
        return this == FAILED || this == DEAD;
    }
}