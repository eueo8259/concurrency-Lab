import org.example.domain.Course;
import org.example.domain.ReentrantLockCourse;
import org.example.domain.SynchronizedCourse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseRegistrationTest {

    @Test
    void SynchronizedCourse_수강신청_100명정원_500명동시요청_정합성테스트() throws InterruptedException {
        // Given
        int totalRequests = 500;
        Course course = new SynchronizedCourse();
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // When
        for (int i = 0; i < totalRequests; i++) {
            executorService.submit(() -> {
                try {
                    if (course.enroll()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드 작업 완료 대기

        // Then
        assertEquals(100, course.getEnrollmentCount(), "최종 등록 인원은 100명이어야 합니다.");
        assertEquals(100, successCount.get(), "성공 횟수는 100번이어야 합니다.");
        assertEquals(400, failCount.get(), "실패 횟수는 400번이어야 합니다.");
    }

    @Test
    void ReentrantLockCourse_수강신청_100명정원_500명동시요청_정합성테스트() throws InterruptedException {
        // Given
        int totalRequests = 500;
        Course course = new ReentrantLockCourse();
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // When
        for (int i = 0; i < totalRequests; i++) {
            executorService.submit(() -> {
                try {
                    if (course.enroll()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드 작업 완료 대기

        // Then
        assertEquals(100, course.getEnrollmentCount(), "최종 등록 인원은 100명이어야 합니다.");
        assertEquals(100, successCount.get(), "성공 횟수는 100번이어야 합니다.");
        assertEquals(400, failCount.get(), "실패 횟수는 400번이어야 합니다.");
    }

}