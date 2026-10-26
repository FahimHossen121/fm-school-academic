-- Explicit opt-in local demo data.
-- Admin password: 12345678. Other demo passwords equal their usernames.
-- Re-running preserves demo account passwords and profiles, except the demo Admin
-- is deliberately reset to 12345678 so the lab login is always known. No DELETE statements.
-- Run only after migrations 001-005 have applied. Conflicting course assignments abort the transaction.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO accounts (username, password_hash, role)
VALUES ('admin', '{bcrypt}' || crypt('12345678', gen_salt('bf')), 'ADMIN')
ON CONFLICT (username) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = 'ADMIN';
INSERT INTO accounts (username, password_hash, role) VALUES ('teacher2', '{bcrypt}$2a$10$kh9f3BbqmMo74rqJs3P9RuwD3JI6RxufBlbfYjMN03iF/DCD1brrq', 'TEACHER') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('teacher3', '{bcrypt}$2a$10$QCItVddR4r62fFnQxBr9xuHavDjhlLWMRg3XiIDr7HeyDg.kdLaMi', 'TEACHER') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('teacher4', '{bcrypt}$2a$10$N9BORmUgVM8mPp.n3hc/C.MZlHD2Sp4kNhMfr/HfI0E8/uZT/0R3a', 'TEACHER') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('teacher5', '{bcrypt}$2a$10$yRitUK9bj9odk/GC4KcQguEBOGgs6MO1z293M1Eky4wikVopT1VmK', 'TEACHER') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('teacher6', '{bcrypt}$2a$10$9aQMcgJXLHSXFRSJxTbwI.BYilCDzE7TsNezig1AD31Gv6XKN7GPa', 'TEACHER') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student2', '{bcrypt}$2a$10$lZ0k3jxwYOmyQgok.MS4zei6ganoSwJ/2HDNh1rBd6oF/XaXSEGRe', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student3', '{bcrypt}$2a$10$siZT/xtcQwX.wzVCiIyxceSJVXEA0paDK0OA59kxzBSFmxczR1dLm', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student4', '{bcrypt}$2a$10$xh7D/cDqZCuMsivlBCfh.uK.DXzkr.w2qf3/pS2h3YLLEaektrnK.', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student5', '{bcrypt}$2a$10$S82DvBvocBKNhPmmsS1ezuzvQA86Z8vISBw9ojAv7x6ESc3e2..jW', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student6', '{bcrypt}$2a$10$5QD5yQcGZwwf7MVZNFJqvOptxRJc7NR0rJriGvS7MJHeizzx8L8Fe', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student7', '{bcrypt}$2a$10$ObZgqNOELQyN/k5dn7Ik5e3CfUNWfANdPMQiFdqmTqu89FAd0V7ju', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student8', '{bcrypt}$2a$10$3D1Y8eC5f1UsFGbtDeuD6uWfdhybDvTwic8nOqoI6wU0bD/HFzi6W', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student9', '{bcrypt}$2a$10$h6oJkUADAfALafcA1cMiZu19N.ccury8w.QDFGypUTeBDxGUZVhWe', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student10', '{bcrypt}$2a$10$kwsPWde/AHnHobtQzMdMbeGcyioFKEZ3Md0lDZuZ0gdLVi93sFzLy', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student11', '{bcrypt}$2a$10$BpJ1ZsBSBTRxrWcTsjEW3uGnufrAnHL3qroTIKkt5Mji3Iha.rR1q', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student12', '{bcrypt}$2a$10$tOfTGlPsY8UkP.rM/VJ1D.kXBC.LqMaEfz0760BRAgDHv.CsrX8S.', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student13', '{bcrypt}$2a$10$/c5INGrLB6Wc2DICpHtIw.YXRiKHNsoPVUNZwbpAHxg2wmyjWHnTK', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student14', '{bcrypt}$2a$10$.EE1BAO4mcA6Od59KNPMxOkEDS1Bub5A7tXVEwQMedH7/SilR6JLi', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student15', '{bcrypt}$2a$10$psOCtKces/YKt1rlvJYPmeKZcHgzSjnVvPi95LOWQpXkKO.Oy/gk2', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO accounts (username, password_hash, role) VALUES ('student16', '{bcrypt}$2a$10$I1H55AcBwG8/3Z6uBWBkiep.eDWYx8MlclLZFw1Dhwzuy/8QV9SEa', 'STUDENT') ON CONFLICT (username) DO NOTHING;
INSERT INTO teachers (account_id, name, course) SELECT id, 'teacher2', 'Demo Course 2' FROM accounts WHERE username='teacher2' AND role='TEACHER' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO teachers (account_id, name, course) SELECT id, 'teacher3', 'Demo Course 3' FROM accounts WHERE username='teacher3' AND role='TEACHER' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO teachers (account_id, name, course) SELECT id, 'teacher4', 'Demo Course 4' FROM accounts WHERE username='teacher4' AND role='TEACHER' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO teachers (account_id, name, course) SELECT id, 'teacher5', 'Demo Course 5' FROM accounts WHERE username='teacher5' AND role='TEACHER' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO teachers (account_id, name, course) SELECT id, 'teacher6', 'Demo Course 6' FROM accounts WHERE username='teacher6' AND role='TEACHER' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student2', '9000002' FROM accounts WHERE username='student2' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher2' AND sa.username='student2' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student3', '9000003' FROM accounts WHERE username='student3' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher2' AND sa.username='student3' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student4', '9000004' FROM accounts WHERE username='student4' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher2' AND sa.username='student4' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student5', '9000005' FROM accounts WHERE username='student5' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher3' AND sa.username='student5' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student6', '9000006' FROM accounts WHERE username='student6' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher3' AND sa.username='student6' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student7', '9000007' FROM accounts WHERE username='student7' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher3' AND sa.username='student7' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student8', '9000008' FROM accounts WHERE username='student8' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher4' AND sa.username='student8' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student9', '9000009' FROM accounts WHERE username='student9' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher4' AND sa.username='student9' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student10', '9000010' FROM accounts WHERE username='student10' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher4' AND sa.username='student10' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student11', '9000011' FROM accounts WHERE username='student11' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher5' AND sa.username='student11' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student12', '9000012' FROM accounts WHERE username='student12' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher5' AND sa.username='student12' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student13', '9000013' FROM accounts WHERE username='student13' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher5' AND sa.username='student13' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student14', '9000014' FROM accounts WHERE username='student14' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher6' AND sa.username='student14' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student15', '9000015' FROM accounts WHERE username='student15' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher6' AND sa.username='student15' ON CONFLICT (teacher_id,student_id) DO NOTHING;
INSERT INTO students (account_id, name, roll) SELECT id, 'student16', '9000016' FROM accounts WHERE username='student16' AND role='STUDENT' ON CONFLICT (account_id) DO NOTHING;
INSERT INTO enrollments (teacher_id, student_id, status, requested_by) SELECT t.id, s.id, 'ACCEPTED', 'ADMIN' FROM teachers t JOIN accounts ta ON ta.id=t.account_id CROSS JOIN students s JOIN accounts sa ON sa.id=s.account_id WHERE ta.username='teacher6' AND sa.username='student16' ON CONFLICT (teacher_id,student_id) DO NOTHING;
COMMIT;
