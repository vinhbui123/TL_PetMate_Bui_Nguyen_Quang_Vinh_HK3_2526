# Development Guide — petAppServer

This file explains how to run the backend locally, generate the Maven wrapper, and apply DB migrations.

Prerequisites
- Java 17+ installed and `java` on PATH
- Maven installed (`mvn`) OR use the Maven wrapper (see below)
- A running database configured in `src/main/resources/application.yml` (Postgres/MySQL). Create the DB and ensure credentials match.

Generate Maven Wrapper (one-time)
1. On a machine with Maven installed run:

```powershell
cd D:\petAppServer
.\generate_maven_wrapper.ps1
```

2. Commit the generated `mvnw`, `mvnw.cmd` and `.mvn/wrapper` files.

Build and run

Using installed Maven:
```powershell
cd D:\petAppServer
mvn -DskipTests package
mvn spring-boot:run
# or
java -jar target/petAppServer-1.0-SNAPSHOT.jar
```

Using the wrapper (after generating/committing it):
```powershell
cd D:\petAppServer
.\mvnw -DskipTests package
.\mvnw spring-boot:run
```

Apply DB migrations
- Migrations are applied automatically on application startup via Flyway (files in `src/main/resources/db/migration`).
- Check logs for Flyway output to confirm `V1.27.0__create_organization_profiles.sql` applied.

Verify migration manually (example Postgres):
```sql
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'organization_profiles';
SELECT * FROM organization_profiles LIMIT 10;
```

Troubleshooting
- If Flyway fails, ensure DB user has CREATE/ALTER permissions.
- If you cannot run Maven locally, push your branch and CI (GitHub Actions) will attempt a build.
