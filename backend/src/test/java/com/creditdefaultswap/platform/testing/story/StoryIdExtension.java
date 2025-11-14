package com.creditdefaultswap.platform.testing.story;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Optional;

/**
 * JUnit 5 extension that applies Allure labels derived from {@link StoryId} annotations.
 */
class StoryIdExtension implements BeforeEachCallback {

    private final AllureLifecycle lifecycle;

    StoryIdExtension() {
        this(Allure.getLifecycle());
    }

    StoryIdExtension(AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Optional<StoryId> storyId = resolveStoryId(context);
        storyId.ifPresent(annotation -> lifecycle.updateTestCase(
            testResult -> StoryLabelApplier.applyLabels(testResult, annotation)
        ));
    }

    private Optional<StoryId> resolveStoryId(ExtensionContext context) {
        Optional<StoryId> direct = context.getElement()
            .flatMap(element -> AnnotationSupport.findAnnotation(element, StoryId.class));

        if (direct.isPresent()) {
            return direct;
        }

        return context.getTestClass()
            .flatMap(testClass -> AnnotationSupport.findAnnotation(testClass, StoryId.class));
    }
}
