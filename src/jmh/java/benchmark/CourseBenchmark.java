package benchmark;

import org.example.EnrollmentBenchmarkApplication;
import org.example.application.EnrollmentService;
import org.example.domain.Course;
import org.example.domain.Student;
import org.example.repository.CourseRepository;
import org.example.repository.EnrollmentRepository;
import org.example.repository.StudentRepository;
import org.openjdk.jmh.annotations.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(32)
public class CourseBenchmark {

    private ConfigurableApplicationContext context;
    private EnrollmentService enrollmentService;
    private EnrollmentRepository enrollmentRepository;

    private Long targetCourseId;
    private final AtomicLong studentIdCounter = new AtomicLong(1);

    @Setup(Level.Trial)
    public void init() {
        // 1. Spring Context 구동 및 Bean 주입
        context = SpringApplication.run(EnrollmentBenchmarkApplication.class);
        enrollmentService = context.getBean(EnrollmentService.class);
        enrollmentRepository = context.getBean(EnrollmentRepository.class);
        CourseRepository courseRepository = context.getBean(CourseRepository.class);
        StudentRepository studentRepository = context.getBean(StudentRepository.class);

        // 2. 테스트용 강의(Course) 생성
        Course course = new Course("동시성 테스트 강의");
        targetCourseId = courseRepository.save(course).getId();

        // 3. 테스트용 학생(Student) 대량 생성
        List<Student> batchStudents = new ArrayList<>();
        for (int i = 1; i <= 100000; i++) {
            batchStudents.add(new Student("학생" + i));
            if (i % 5000 == 0) {
                studentRepository.saveAll(batchStudents);
                batchStudents.clear();
            }
        }
    }

    @Setup(Level.Iteration)
    public void cleanEnrollments() {
        // 이전 측정 결과 삭제 (정원 0명으로 리셋)
        enrollmentRepository.deleteAllInBatch();
        // 매 Iteration마다 1번 학생부터 다시 시작하도록 카운터 리셋
        studentIdCounter.set(1);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Benchmark
    public void measureSynchronized() {
        enrollmentService.enrollWithSync(studentIdCounter.getAndIncrement(), targetCourseId);
    }

    @Benchmark
    public void measureReentrantLock() {
        enrollmentService.enrollWithLock(studentIdCounter.getAndIncrement(), targetCourseId);
    }
}