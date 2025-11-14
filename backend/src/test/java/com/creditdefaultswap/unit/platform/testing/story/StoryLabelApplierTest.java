package com.creditdefaultswap.unit.platform.testing.story;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoryLabelApplierTest {

    @Test
    void appliesStoryTestTypeAndServiceLabels() throws Exception {
        Method sampleMethod = SampleAnnotatedTests.class.getDeclaredMethod("sampleUnitTest");
        StoryId annotation = sampleMethod.getAnnotation(StoryId.class);

        TestResult result = new TestResult();
        StoryLabelApplier.applyLabels(result, annotation);

        Map<String, String> labels = result.getLabels()
            .stream()
            .collect(Collectors.toMap(Label::getName, Label::getValue));

    assertEquals("UTS-150", labels.get("story"));
    assertEquals("unit", labels.get("testType"));
    assertEquals("backend", labels.get("service"));
    assertEquals("cds-platform", labels.get("microservice"));
    }

    @Test
    void methodAnnotationOverridesClassLevelDefaults() throws Exception {
        Method sampleMethod = SampleAnnotatedTests.class.getDeclaredMethod("sampleContractTest");
        StoryId annotation = sampleMethod.getAnnotation(StoryId.class);

        TestResult result = new TestResult();
        StoryLabelApplier.applyLabels(result, annotation);

        Map<String, String> labels = result.getLabels()
            .stream()
            .collect(Collectors.toMap(Label::getName, Label::getValue));

    assertEquals("UTS-151", labels.get("story"));
    assertEquals("contract", labels.get("testType"));
    assertEquals("backend", labels.get("service"));
    assertEquals("risk-engine", labels.get("microservice"));
    }

    @StoryId(value = "UTS-149", microservice = "cds-platform")
    private static class SampleAnnotatedTests {

        @StoryId(value = "UTS-150", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
        static void sampleUnitTest() {
            // Intentionally empty: the method exists only to host the StoryId annotation for reflection.
        }

        @StoryId(value = "UTS-151", testType = StoryId.TestType.CONTRACT, microservice = "risk-engine")
        static void sampleContractTest() {
            // Intentionally empty: the method exists only to host the StoryId annotation for reflection.
        }
    }
}
