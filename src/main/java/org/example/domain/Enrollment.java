package org.example.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@Entity
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "enrollment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_course",
                        columnNames = {"student_id", "course_id"}
                )
        }
)
public class Enrollment {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    public static Enrollment create(Student student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.student = student;
        enrollment.course = course;
        enrollment.status = EnrollmentStatus.COMPLETED;
        return enrollment;
    }
}