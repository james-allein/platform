package io.fundrequest.core.notification.dto;

import io.fundrequest.core.notification.domain.NotificationType;

import java.time.LocalDateTime;

public abstract class NotificationDto {

    private Long id;
    private NotificationType type;
    private LocalDateTime date;

    public NotificationDto(Long id, NotificationType type, LocalDateTime date) {
        this.id = id;
        this.type = type;
        this.date = date;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
