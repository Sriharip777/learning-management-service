package com.tcon.learning_management_service.worksheet.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QuestionBankIntegrationService {

    public void validateQuestion(
            String questionMasterId,
            String questionVersionId
    ) {

        log.info(
                "Validating Question master={}, version={}",
                questionMasterId,
                questionVersionId
        );

        // FUTURE:
        // questionBankClient.getQuestion(...)
    }
}