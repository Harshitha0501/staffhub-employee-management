<p align="center">
<img src="imagesstaffhub-banner.png" alt="StaffHub Employee Management System Banner" width="100%">
</p>

<p align="center">
  <a href="https://staffhub-employee-management.onrender.com">
    <img src="https://img.shields.io/badge/Live_Demo-Visit_StaffHub-2563EB?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Live Demo">
  </a>
</p>

# StaffHub — Employee Management System

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot" />
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity" />
  <img src="https://img.shields.io/badge/H2-Database-blue" />
  <img src="https://img.shields.io/badge/HTML5-E34F26?logo=html5&logoColor=white" />
  <img src="https://img.shields.io/badge/CSS3-1572B6?logo=css3&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaScript-F7DF1E?logo=javascript&logoColor=black" />
  <img src="https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white" />
</p>


## Features

### Admin
- View company dashboard and statistics.
- Manage employees and departments.
- Create HR accounts and manage staff accounts.
- Manage tasks, attendance, leave, payroll, and announcements.

### HR
- View employee records.
- Manage attendance and payroll.
- Assign tasks and manage leave requests.
- Post announcements.

### Employee
- Access a personal dashboard.
- Check in and check out.
- Apply for leave and track requests.
- View payslips, tasks, announcements, and notifications.

## Technologies
- Java 17
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- H2 Database
- HTML, CSS, JavaScript
- Maven

## Run the Application

### Prerequisites
- JDK 17 or later
- Maven 3.9 or later

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/Harshitha0501/staffhub-employee-management.git
   ```
2. Open the project folder containing `pom.xml`.
3. Run:
   ```bash
   mvn spring-boot:run
   ```
4. Open http://localhost:8080 in your browser.

The application uses an in-memory H2 database. Data resets when the application restarts.

## Project Structure

```
src/
└── main/
    ├── java/com/staffhub/
    │   ├── config/
    │   ├── web/
    │   ├── service/
    │   ├── model/
    │   ├── repository/
    │   ├── dto/
    │   └── exception/
    └── resources/static/
        ├── index.html
        ├── app.html
        ├── css/
        └── js/

pom.xml
README.md
```

## Author

**Harshitha C.**

- GitHub: https://github.com/Harshitha0501
- Project: https://github.com/Harshitha0501/staffhub-employee-management

## 📸 Project Screenshots

### 1. Admin Dashboard
![Admin Dashboard](admin-dashboard.png)

### 2. HR Dashboard
![HR Dashboard](hr-dashboard.png)

### 3. Employee Dashboard
![Employee Dashboard](employee-dashboard.png)

### 4. Attendance
![Attendance](attendance.png)

### 5. Announcements
![Announcements](announcements.png)

### 6. Payslip
![Payslip](payslip.png)

### 7. Task Assigned
![Task Assigned](task-assigned.png)

### 8. Leave Status
![Leave Status](leave-status.png)

### 9. HR Leave Requests
![HR Leave Requests](hr-leave-requests.png)

### 10. Employee Management
![Employee Management](employee-management.png)

### 11. Department Management
![Department Management](department-management.png)
