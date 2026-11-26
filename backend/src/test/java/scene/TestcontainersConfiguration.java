package scene;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트마다 빈 PostgreSQL 컨테이너를 띄우고 Spring 접속 정보에 연결한다.
 * 같은 설정을 쓰는 테스트는 Spring 컨텍스트 캐시 덕분에 컨테이너 하나를 같이 쓴다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** compose.yaml과 같은 버전으로 맞춘다. */
    static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18.7-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
