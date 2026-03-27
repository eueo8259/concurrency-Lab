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

    private final int MAX_CAPACITY = 100;

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    private final Lock reentrantLock = new ReentrantLock();

    @Transactional
    public boolean enroll(Long studentId, Long courseId) {
        long count = enrollmentRepository.countByCourseId(courseId);

        if (count < MAX_CAPACITY) {
            Course course = courseRepository.getReferenceById(courseId);
            Student student = studentRepository.getReferenceById(studentId);

            Enrollment enrollment = Enrollment.create(student, course);
            enrollmentRepository.save(enrollment);
            return true;
        }
        return false;
    }

    @Transactional
    public void enrollWithPessimisticLock(Long studentId, Long courseId) {
        // 1. Course에 비관적 락을 걸어 조회 (다른 트랜잭션의 접근을 차단)
        Course course = courseRepository.findByIdWithPessimisticLock(courseId)
                .orElseThrow();

        // 2. 락이 걸린 상태에서 카운트 조회 (가시성 보장)
        long count = enrollmentRepository.countByCourseId(courseId);

        if (count < 100) {
            Student student = studentRepository.getReferenceById(studentId);
            Enrollment enrollment = Enrollment.create(student, course);
            enrollmentRepository.save(enrollment);
        }
    }
}