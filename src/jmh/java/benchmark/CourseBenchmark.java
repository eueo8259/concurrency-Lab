package benchmark;

import org.example.EnrollmentBenchmarkApplication;
import org.example.application.EnrollmentFacade;
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
    private EnrollmentService enrollmentService;
    private EnrollmentRepository enrollmentRepository;

    // 락 방식별로 독립된 강의 ID (격리)
    private Long courseIdSync;
    private Long courseIdLock;
    private Long courseIdPessimistic;

    private List<Long> studentIds;
    private final AtomicInteger indexCounter = new AtomicInteger(0);

    @Setup(Level.Trial)
    public void init() {
        context = SpringApplication.run(EnrollmentBenchmarkApplication.class);
        enrollmentFacade = context.getBean(EnrollmentFacade.class);
        enrollmentService = context.getBean(EnrollmentService.class);
        enrollmentRepository = context.getBean(EnrollmentRepository.class);

        CourseRepository courseRepository = context.getBean(CourseRepository.class);
        StudentRepository studentRepository = context.getBean(StudentRepository.class);

        // 초기화
        enrollmentRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();

        // 1. 각 방식이 사용할 독립된 강의 생성
        courseIdSync = courseRepository.save(new Course("Synchronized 강의")).getId();
        courseIdLock = courseRepository.save(new Course("ReentrantLock 강의")).getId();
        courseIdPessimistic = courseRepository.save(new Course("PessimisticLock 강의")).getId();

        // 2. 테스트용 학생 대량 생성 (ID 공유는 조회 연산이므로 오염 위험 낮음)
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= 100000; i++) {
            students.add(new Student("학생" + i));
        }
        List<Student> savedStudents = studentRepository.saveAll(students);
        studentIds = savedStudents.stream().map(Student::getId).collect(Collectors.toList());
    }

    @Setup(Level.Iteration)
    public void clean() {
        // 매 반복마다 수강신청 내역을 비워 정원(100명)까지의 경합을 공정하게 반복 측정
        enrollmentRepository.deleteAllInBatch();
        indexCounter.set(0);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) context.close();
    }

    @Benchmark
    public void measureReentrantLock() {
        int idx = indexCounter.getAndIncrement() % studentIds.size();
        enrollmentFacade.enrollWithLock(studentIds.get(idx), courseIdLock);
    }

    @Benchmark
    public void measureSynchronized() {
        int idx = indexCounter.getAndIncrement() % studentIds.size();
        enrollmentFacade.enrollWithSync(studentIds.get(idx), courseIdSync);
    }

    @Benchmark
    public void measurePessimisticLock() {
        int idx = indexCounter.getAndIncrement() % studentIds.size();
        enrollmentService.enrollWithPessimisticLock(studentIds.get(idx), courseIdPessimistic);
    }
}