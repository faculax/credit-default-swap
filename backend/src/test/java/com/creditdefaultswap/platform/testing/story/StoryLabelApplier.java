package com.creditdefaultswap.platform.testing.story;

import com.creditdefaultswap.testing.validation.LabelValidator;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;

import java.util.Objects;

/**
 * Utility class that applies canonical labels to an Allure {@link TestResult}.
 * Validates labels against the unified label schema.
 */
final class StoryLabelApplier {

    private static final String STORY_LABEL = "story";
    private static final String TEST_TYPE_LABEL = "testType";
    private static final String SERVICE_LABEL = "service";
    private static final String MICROSERVICE_LABEL = "microservice";
    
    private static final LabelValidator VALIDATOR = new LabelValidator();

    private StoryLabelApplier() {
    }

    static void applyLabels(TestResult result, StoryId annotation) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(annotation, "annotation");

        // Validate labels before applying
        String testType = annotation.testType().labelValue();
        String service = annotation.service().labelValue();
        String microservice = annotation.microservice();
        
        try {
            VALIDATOR.validateLabels(testType, service, microservice);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Invalid @StoryId annotation on test: " + e.getMessage(), e
            );
        }

        setLabel(result, STORY_LABEL, annotation.value());
        setLabel(result, TEST_TYPE_LABEL, testType);
        setLabel(result, SERVICE_LABEL, service);
        setLabel(result, MICROSERVICE_LABEL, microservice);
    }

    private static void setLabel(TestResult result, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        result.getLabels().removeIf(label -> name.equals(label.getName()));
        result.getLabels().add(new Label().setName(name).setValue(value));
    }
}

