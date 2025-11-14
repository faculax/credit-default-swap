package com.creditdefaultswap.unit.platform.testing.story;

import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical story identifier annotation that applies Allure story, test type, service, and microservice labels.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({AllureJunit5.class, StoryIdExtension.class})
public @interface StoryId {

    String value();

    TestType testType() default TestType.UNIT;

    Service service() default Service.BACKEND;

    String microservice() default "";

    enum TestType {
        UNIT("unit"),
        INTEGRATION("integration"),
        CONTRACT("contract");

        private final String labelValue;

        TestType(String labelValue) {
            this.labelValue = labelValue;
        }

        public String labelValue() {
            return labelValue;
        }
    }

    enum Service {
        BACKEND("backend"),
        FRONTEND("frontend");

        private final String labelValue;

        Service(String labelValue) {
            this.labelValue = labelValue;
        }

        public String labelValue() {
            return labelValue;
        }
    }
}
