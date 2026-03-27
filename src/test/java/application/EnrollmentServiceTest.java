package application;

import org.example.EnrollmentBenchmarkApplication;
import org.example.application.EnrollmentService;
import org.example.domain.Course;
import org.example.domain.Student;
import org.example.repository.CourseRepository;
import org.example.repository.EnrollmentRepository;
import org.example.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EnrollmentBenchmarkApplication.class)
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    private Long courseId;

    @BeforeEach
    void setUp() {
        // 1. 기존 데이터 초기화
        enrollmentRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch(); // 학생 중복 생성 방지

        Course course = courseRepository.save(new Course("동시성 테스트 강의"));
        courseId = course.getId();

        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            students.add(new Student("학생" + i));
        }
        studentRepository.saveAll(students);
    }
    @Test
    void test1() throws InterruptedException {
        // given
        int threadCount = 120;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (long i = 1; i <= threadCount; i++) {
            long studentId = i;
            executorService.submit(() -> {
                try {
                    // synchronized 메서드 호출
                    enrollmentService.enrollWithSync(studentId, courseId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long count = enrollmentRepository.countByCourseId(courseId);
        
        System.out.println("최종 등록 인원: " + count);
        assertThat(count).isNotEqualTo(100);
    }

    @Test
    void test2() throws InterruptedException {
        // given
        int threadCount = 120;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (long i = 1; i <= threadCount; i++) {
            long studentId = i;
            executorService.submit(() -> {
                try {
                    // lock 메서드 호출
                    enrollmentService.enrollWithLock(studentId, courseId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long count = enrollmentRepository.countByCourseId(courseId);

        System.out.println("최종 등록 인원: " + count);
        assertThat(count).isNotEqualTo(100);
    }
}