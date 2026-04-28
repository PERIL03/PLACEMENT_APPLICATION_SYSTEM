INSERT INTO student (full_name, email, branch, graduation_year, cgpa)
SELECT 'Aarav Mehta', 'aarav.m@college.edu', 'Computer Science', 2026, 8.7
WHERE NOT EXISTS (SELECT 1 FROM student WHERE email = 'aarav.m@college.edu');

INSERT INTO student (full_name, email, branch, graduation_year, cgpa)
SELECT 'Riya Sharma', 'riya.s@college.edu', 'Information Technology', 2025, 9.1
WHERE NOT EXISTS (SELECT 1 FROM student WHERE email = 'riya.s@college.edu');

INSERT INTO company (name, website, location)
SELECT 'TechNova', 'https://technova.example', 'Bengaluru'
WHERE NOT EXISTS (SELECT 1 FROM company WHERE name = 'TechNova');

INSERT INTO company (name, website, location)
SELECT 'InfiSoft Labs', 'https://infisoft.example', 'Pune'
WHERE NOT EXISTS (SELECT 1 FROM company WHERE name = 'InfiSoft Labs');

INSERT INTO drive (company_id, role_title, eligible_branch, min_cgpa, package_lpa, application_deadline)
SELECT c.id, 'Software Engineer', 'Computer Science', 7.5, 10.0, '2026-07-30'
FROM company c
WHERE c.name = 'TechNova'
    AND NOT EXISTS (
            SELECT 1 FROM drive d
            WHERE d.role_title = 'Software Engineer' AND d.company_id = c.id
    );

INSERT INTO drive (company_id, role_title, eligible_branch, min_cgpa, package_lpa, application_deadline)
SELECT c.id, 'Backend Developer', 'Information Technology', 8.0, 12.5, '2026-08-10'
FROM company c
WHERE c.name = 'InfiSoft Labs'
    AND NOT EXISTS (
            SELECT 1 FROM drive d
            WHERE d.role_title = 'Backend Developer' AND d.company_id = c.id
    );
