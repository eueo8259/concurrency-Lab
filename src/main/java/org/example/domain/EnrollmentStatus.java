package org.example.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentStatus {
    COMPLETED("신청 완료"),

    CANCELLED("신청 취소"),

    PENDING("대기 중"),

    REJECTED("신청 반려");

    private final String description;
}
