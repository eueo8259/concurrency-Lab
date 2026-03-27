package application;

import org.example.EnrollmentBenchmarkApplication;
import org.example.application.EnrollmentFacade;
import org.example.domain.Course;
import org.example.domain.Student;
import org.example.repository.CourseRepository;
import org.example.repository.EnrollmentRepository;
import org.example.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EnrollmentBenchmarkApplication.class)
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentFacade enrollmentFacade;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    private Long courseId;
    private List<Long> studentIds; // 실제 생성된 학생 ID 리스트
    private ExecutorService executorService;

    private final int THREAD_COUNT = 120;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();

        Course course = courseRepository.save(new Course("동시성 테스트 강의"));
        courseId = course.getId();

        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= THREAD_COUNT; i++) {
            students.add(new Student("학생" + i));
        }
        List<Student> savedStudents = studentRepository.saveAll(students);

        studentIds = savedStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        executorService = Executors.newFixedThreadPool(64);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    @DisplayName("Facade + synchronized: 전체 테스트 실행 시에도 정합성 보장")
    void test1() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            Long studentId = studentIds.get(i);
            executorService.submit(() -> {
                try {
                    enrollmentFacade.enrollWithSync(studentId, courseId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long count = enrollmentRepository.countByCourseId(courseId);
        System.out.println("synchronized 최종 등록 인원: " + count);
        assertThat(count).isEqualTo(100);
    }

    @Test
    @DisplayName("Facade + ReentrantLock: 전체 테스트 실행 시에도 정합성 보장")
    void test2() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            Long studentId = studentIds.get(i);
            executorService.submit(() -> {
                try {
                    enrollmentFacade.enrollWithLock(studentId, courseId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long count = enrollmentRepository.countByCourseId(courseId);
        System.out.println("ReentrantLock 최종 등록 인원: " + count);
        assertThat(count).isEqualTo(100);
    }
}