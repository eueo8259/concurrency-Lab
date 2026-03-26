package org.example.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockCourse implements Course {
    private static final int MAX_CAPACITY = 100;
    private int enrollmentCount = 0;
    private final Lock lock = new ReentrantLock();

    @Override
    public boolean enroll() {
        lock.lock();
        try {
            if (enrollmentCount < MAX_CAPACITY) {
                enrollmentCount++;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getEnrollmentCount() {
        return enrollmentCount;
    }
}