package org.example.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.domain.Course;
import org.example.domain.Enrollment;
import org.example.domain.Student;
import org.example.repository.CourseRepository;
import org.example.repository.EnrollmentRepository;
import org.example.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    //TODO: 파사드 패턴으로 수정하기
    private final int MAX_CAPACITY = 100;

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    private final Lock reentrantLock = new ReentrantLock();

    // 방법 1: synchronized 적용
    @Transactional
    public synchronized void enrollWithSync(Long studentId, Long courseId) {
        processEnrollment(studentId, courseId);
    }

    // 방법 2: ReentrantLock 적용
    @Transactional
    public void enrollWithLock(Long studentId, Long courseId) {
        reentrantLock.lock();
        try {
            processEnrollment(studentId, courseId);
        } finally {
            reentrantLock.unlock();
        }
    }


    private void processEnrollment(Long studentId, Long courseId) {
        long count = enrollmentRepository.countByCourseId(courseId);

        if (count < MAX_CAPACITY) {
            Course course = courseRepository.getReferenceById(courseId);
            Student student = studentRepository.getReferenceById(studentId);

            Enrollment enrollment = Enrollment.create(student, course);
            enrollmentRepository.save(enrollment);
        }
    }
}