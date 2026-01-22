package com.tcon.learning_management_service.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableMongoRepositories(
        basePackages = {
                "com.tcon.learning_management_service.course.repository",
                "com.tcon.learning_management_service.session.repository",
                "com.tcon.learning_management_service.booking.repository",
                "com.tcon.learning_management_service.demo.repository",
                "com.tcon.learning_management_service.availability.repository"
        }
)
public class DatabaseConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder ->
                        builder.maxSize(100)
                                .minSize(10)
                                .maxWaitTime(2000, TimeUnit.MILLISECONDS)
                )
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                                .readTimeout(10000, TimeUnit.MILLISECONDS)
                )
                .build();

        log.info("MongoDB client configured successfully");
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient, MappingMongoConverter converter) {
        // Remove _class field from documents
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return new MongoTemplate(mongoClient, getDatabaseName());
    }
}
