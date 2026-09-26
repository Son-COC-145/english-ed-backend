package com.example.english_app.service.classroom;

import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Centralizes authorization rules that depend on persisted course relationships. */
@Service
@RequiredArgsConstructor
public class CourseAccessService {
    private final UserRepository userRepository;
    private final CourseStudentRepository courseStudentRepository;

    public User requireTeacherOrAdmin(Long actorId, Course course) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        if (actor.getRole() != Role.ADMIN
                && (actor.getRole() != Role.TEACHER || !course.getTeacher().getId().equals(actorId))) {
            throw ErrorCode.COURSE_ACCESS_DENIED.toException();
        }
        return actor;
    }

    /** Assignments and grading belong to the course's own teacher only; admins are intentionally excluded. */
    public User requireCourseTeacher(Long actorId, Course course) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        if (actor.getRole() != Role.TEACHER || !course.getTeacher().getId().equals(actorId)) {
            throw ErrorCode.COURSE_ACCESS_DENIED.toException();
        }
        return actor;
    }

    public CourseStudent requireActiveStudent(Long studentId, Course course) {
        if (!Boolean.TRUE.equals(course.getIsActive())) {
            throw ErrorCode.COURSE_ACCESS_DENIED.toException();
        }
        CourseStudent enrollment = courseStudentRepository
                .findByCourseIdAndStudentId(course.getId(), studentId)
                .orElseThrow(() -> ErrorCode.STUDENT_NOT_IN_COURSE.toException());
        if (enrollment.getStatus() != ClassStudentStatus.ACTIVE) {
            throw ErrorCode.STUDENT_NOT_IN_COURSE.toException();
        }
        return enrollment;
    }

    public void requireCourseViewer(Long actorId, Course course) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() == Role.TEACHER && course.getTeacher().getId().equals(actorId)) {
            return;
        }
        if (actor.getRole() == Role.STUDENT) {
            requireActiveStudent(actorId, course);
            return;
        }
        throw ErrorCode.COURSE_ACCESS_DENIED.toException();
    }
}
