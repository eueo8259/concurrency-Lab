package org.example.domain;

public class SynchronizedCourse implements Course {
    private static final int MAX_CAPACITY = 100;
    private int enrollmentCount = 0;

    @Override
    public synchronized boolean enroll() {
        if (enrollmentCount < MAX_CAPACITY) {
            enrollmentCount++;
            return true; // 수강신청 성공
        }
        return false; // 수강신청 실패 (정원 초과)
    }

    @Override
    public int getEnrollmentCount() {
        return enrollmentCount;
    }
}