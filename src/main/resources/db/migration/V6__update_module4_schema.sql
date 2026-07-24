-- Update classes table
ALTER TABLE classes 
ADD COLUMN start_date DATE,
ADD COLUMN end_date DATE,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update class_students table
-- 1. Drop existing composite primary key
ALTER TABLE class_students DROP PRIMARY KEY;
-- 2. Add surrogate primary key
ALTER TABLE class_students ADD COLUMN id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY FIRST;
-- 3. Add unique constraint on class_id and student_id
ALTER TABLE class_students ADD CONSTRAINT uk_class_students_class_id_student_id UNIQUE (class_id, student_id);
-- 4. Add updated_at
ALTER TABLE class_students ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update teaching_materials table
ALTER TABLE teaching_materials ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update assignments table
ALTER TABLE assignments 
ADD COLUMN description TEXT,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update assignment_submissions table
ALTER TABLE assignment_submissions 
ADD COLUMN score DECIMAL(5,2),
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update syllabus_items table
ALTER TABLE syllabus_items ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
