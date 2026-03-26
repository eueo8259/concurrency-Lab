package benchmark;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(32) // 32개의 스레드가 지속적으로 경합 발생
public class CourseBenchmark {

    private Course syncCourse;
    private Course lockCourse;

    @Setup(Level.Iteration)
    public void setup() {
        // 매 Iteration 시작 시 객체 초기화 (정원 0명으로 리셋)
        syncCourse = new SynchronizedCourse();
        lockCourse = new ReentrantLockCourse();
    }

    @Benchmark
    public void measureSynchronized() {
        syncCourse.enroll();
    }

    @Benchmark
    public void measureReentrantLock() {
        lockCourse.enroll();
    }
}