SELECT e1.emp_id,
       e1.first_name,
       e1.last_name,
       d.department_name,
       COUNT(e2.emp_id) AS younger_employees_count
FROM employee e1
JOIN department d ON e1.department = d.department_id
LEFT JOIN employee e2
  ON e1.department = e2.department
  AND e2.dob > e1.dob
GROUP BY e1.emp_id, e1.first_name, e1.last_name, d.department_name
ORDER BY e1.emp_id DESC;


