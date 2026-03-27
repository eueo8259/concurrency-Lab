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

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    private final Lock reentrantLock = new ReentrantLock();

    @Transactional
    public boolean enroll(Long studentId, Long courseId) {
        long count = enrollmentRepository.countByCourseId(courseId);

        int MAX_CAPACITY = 100;
        if (count < MAX_CAPACITY) {
            Course course = courseRepository.getReferenceById(courseId);
            Student student = studentRepository.getReferenceById(studentId);

            Enrollment enrollment = Enrollment.create(student, course);
            enrollmentRepository.save(enrollment);
            return true;
        }
        return false;
    }
}