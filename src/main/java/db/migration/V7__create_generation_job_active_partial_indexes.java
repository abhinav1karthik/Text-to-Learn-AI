package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__create_generation_job_active_partial_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String databaseProductName = context.getConnection()
                .getMetaData()
                .getDatabaseProductName();

        if (!databaseProductName.toLowerCase().contains("postgresql")) {
            return;
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    create unique index uk_active_course_generation_job_per_user
                        on generation_jobs(user_id)
                        where type = 'COURSE_OUTLINE'
                          and status in ('QUEUED', 'RUNNING')
                    """);

            statement.execute("""
                    create unique index uk_active_lesson_generation_job_per_lesson
                        on generation_jobs(lesson_id)
                        where type = 'LESSON_CONTENT'
                          and status in ('QUEUED', 'RUNNING')
                          and lesson_id is not null
                    """);
        }
    }
}
