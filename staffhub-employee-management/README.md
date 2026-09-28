# StaffHub — Employee Management System

A full-stack HR command center built with **Java 17 · Spring Boot 3.5 · Spring Security · Spring Data JPA (H2) · vanilla HTML/CSS/JS**. Single deployable JAR that serves both the REST API and the frontend.

## What's inside

Role-based access for three roles — **Admin, HR, Employee**:

| Capability | Admin | HR | Employee |
|---|:--:|:--:|:--:|
| Dashboard (company KPIs) | ✓ | ✓ | — |
| Personal dashboard | — | — | ✓ |
| Employee directory + auto-provisioned logins | ✓ | ✓ | — |
| Departments | ✓ | — | — |
| Staff accounts — create HR logins, reset, disable | ✓ | — | — |
| Tasks (assign to anyone) | ✓ | ✓ | own + personal |
| Attendance (full day log) | ✓ | ✓ | own check-in/out |
| Leave (approve / reject) | ✓ | ✓ | apply + track |
| Payroll (generate, adjust, pay) | ✓ | ✓ | own payslips |
| Announcements (post) | ✓ | ✓ | read |
| Notifications | ✓ | ✓ | ✓ |

### Account provisioning (admin generates usernames & passwords)
- **Admin → Accounts** creates **HR** logins — a unique username and a secure random password are generated and shown once.
- **HR/Admin → Employees → Add Employee** auto-provisions an **Employee** login (username `first.last`, generated password), shown once.
- **Reset login / Reset password** regenerates a password (shown once). Passwords are stored **BCrypt-hashed**; the raw value is never persisted or returned again.

### Extra features
- In-app **notifications** (bell + unread badge) for task assignments, leave decisions and new announcements.
- Company **announcements / notice board**.
- **Kanban** task board (To do / In progress / Done) with priority accents.
- Employee **self-service**: apply for leave, check in/out, view own payslips.

## Demo accounts

Login is by **username**.

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| HR | `hr` | `hr12345` |
| Employee | `arjun.mehta` | `welcome123` |

Every seeded employee has a login: `firstname.lastname` / `welcome123` (e.g. `sofia.ramirez`, `priya.nair`, `elena.petrova`).

## Run it

Requirements: **JDK 17+** and **Maven 3.9+**.

```bash
cd staffhub-employee-management
mvn spring-boot:run
```

Then open **http://localhost:8080**. Data is seeded automatically into an in-memory H2 database (resets on restart).

Build a runnable jar:

```bash
mvn clean package
java -jar target/staffhub-1.0.0.jar
```

H2 console (dev): `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:staffhub`, user `sa`, empty password.

## Project layout

```
src/main/java/com/staffhub
├── config/         SecurityConfig, DataLoader (seed)
├── web/            REST controllers (auth, users, employees, tasks, leaves, payroll, attendance, announcements, notifications, dashboard)
├── service/        business logic (provisioning, current-user, notifications, payroll, attendance, dashboard, employee)
├── model/          JPA entities + enums
├── repository/     Spring Data repositories
├── dto/            request/response records
└── exception/      global error handling
src/main/resources/static
├── index.html      login
├── app.html        authenticated shell
├── css/            style.css, extra.css
└── js/             app.js, api.js, ui.js, views/*.js
```
