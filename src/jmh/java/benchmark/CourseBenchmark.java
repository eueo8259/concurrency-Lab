package benchmark;

import org.example.EnrollmentBenchmarkApplication;
import org.example.application.EnrollmentFacade; // Service 대신 Facade 사용
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(32)
public class CourseBenchmark {

    private ConfigurableApplicationContext context;
    private EnrollmentFacade enrollmentFacade;
    private EnrollmentRepository enrollmentRepository;

    private Long targetCourseId;
    private List<Long> studentIds;
    private final AtomicInteger indexCounter = new AtomicInteger(0);

    @Setup(Level.Trial)
    public void init() {
        context = SpringApplication.run(EnrollmentBenchmarkApplication.class);
        enrollmentFacade = context.getBean(EnrollmentFacade.class);
        enrollmentRepository = context.getBean(EnrollmentRepository.class);
        CourseRepository courseRepository = context.getBean(CourseRepository.class);
        StudentRepository studentRepository = context.getBean(StudentRepository.class);

        // 1. 깨끗한 환경을 위해 데이터 초기화
        enrollmentRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();

        // 2. 테스트용 강의 생성
        Course course = courseRepository.save(new Course("JMH 성능 테스트 강의"));
        targetCourseId = course.getId();

        // 3. 테스트용 학생 대량 생성 및 실제 ID 수집
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= 100000; i++) {
            students.add(new Student("학생" + i));
        }

        // saveAll 후 발급된 실제 ID 리스트 확보 (Auto-Increment 대응)
        List<Student> savedStudents = studentRepository.saveAll(students);
        studentIds = savedStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
    }

    @Setup(Level.Iteration)
    public void cleanEnrollments() {
        // 수강 신청 내역만 삭제하여 정원(100명)에 도달하는 과정을 반복 측정
        enrollmentRepository.deleteAllInBatch();
        // ID 리스트를 처음부터 다시 순회하도록 리셋
        indexCounter.set(0);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Benchmark
    public void measureSynchronized() {
        int idx = indexCounter.getAndIncrement() % studentIds.size();
        enrollmentFacade.enrollWithSync(studentIds.get(idx), targetCourseId);
    }

    @Benchmark
    public void measureReentrantLock() {
        int idx = indexCounter.getAndIncrement() % studentIds.size();
        enrollmentFacade.enrollWithLock(studentIds.get(idx), targetCourseId);
    }
}