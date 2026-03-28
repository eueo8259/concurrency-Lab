import org.example.EnrollmentBenchmarkApplication;
import org.example.application.EnrollmentFacade;
import org.example.application.EnrollmentService;
import org.example.domain.Course;
import org.example.domain.Student;
import org.example.repository.CourseRepository;
import org.example.repository.EnrollmentRepository;
import org.example.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SpringBootTest(classes = EnrollmentBenchmarkApplication.class)
class LatencyTest {

    @Autowired
    private EnrollmentFacade enrollmentFacade;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    private List<Long> studentIds;
    private Long courseIdSync;
    private Long courseIdLock;
    private Long courseIdPessimistic;

    private static final int STUDENT_COUNT = 500;
    private static final int THREAD_COUNT = 500;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();

        courseIdSync = courseRepository.save(new Course("Sync 강의")).getId();
        courseIdLock = courseRepository.save(new Course("Lock 강의")).getId();
        courseIdPessimistic = courseRepository.save(new Course("Pessimistic 강의")).getId();

        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            students.add(new Student("학생" + i));
        }
        studentIds = studentRepository.saveAll(students).stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        // JVM 워밍업 - 각 방식을 미리 한 번씩 실행
        enrollmentFacade.enrollWithSync(studentIds.get(0), courseIdSync);
        enrollmentFacade.enrollWithLock(studentIds.get(1), courseIdLock);
        enrollmentService.enrollWithPessimisticLock(studentIds.get(2), courseIdPessimistic);

        // 워밍업 후 enrollment 초기화
        enrollmentRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Synchronized - 500건 요청 지연시간 측정")
    void syncLatency() throws InterruptedException {
        printLatency("Synchronized", THREAD_COUNT, idx ->
                enrollmentFacade.enrollWithSync(studentIds.get(idx), courseIdSync));
    }

    @Test
    @DisplayName("ReentrantLock - 500건 요청 지연시간 측정")
    void lockLatency() throws InterruptedException {
        printLatency("ReentrantLock", THREAD_COUNT, idx ->
                enrollmentFacade.enrollWithLock(studentIds.get(idx), courseIdLock));
    }

    @Test
    @DisplayName("PessimisticLock - 500건 요청 지연시간 측정")
    void pessimisticLatency() throws InterruptedException {
        printLatency("PessimisticLock", THREAD_COUNT, idx ->
                enrollmentService.enrollWithPessimisticLock(studentIds.get(idx), courseIdPessimistic));
    }

    private void printLatency(String label, int threadCount, Consumer<Integer> action) throws InterruptedException {
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount); // 모든 스레드 준비 대기
        CountDownLatch start = new CountDownLatch(1);           // 일제히 시작 신호
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executorService.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 모든 스레드가 준비될 때까지 대기
                    long begin = System.currentTimeMillis();
                    action.accept(idx);
                    latencies.add(System.currentTimeMillis() - begin);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();  // 모든 스레드 준비 완료
        start.countDown(); // 일제히 출발
        done.await();
        executorService.shutdown();

        List<Long> sorted = latencies.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();

        long avg = (long) sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = sorted.get((int) (size * 0.50));
        long p90 = sorted.get((int) (size * 0.90));
        long p99 = sorted.get((int) (size * 0.99));
        long max = sorted.get(size - 1);

        System.out.println("=== " + label + " ===");
        System.out.println("avg : " + avg + "ms");
        System.out.println("p50 : " + p50 + "ms");
        System.out.println("p90 : " + p90 + "ms");
        System.out.println("p99 : " + p99 + "ms");
        System.out.println("max : " + max + "ms");
    }
}