package org.example.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class EnrollmentFacade {

    private final EnrollmentService enrollmentService;
    private final Lock reentrantLock = new ReentrantLock();

    public synchronized void enrollWithSync(Long studentId, Long courseId) {
        enrollmentService.enroll(studentId, courseId);
    }

    public void enrollWithLock(Long studentId, Long courseId) {
        reentrantLock.lock();
        try {
            enrollmentService.enroll(studentId, courseId);
        } finally {
            reentrantLock.unlock();
        }
    }
}