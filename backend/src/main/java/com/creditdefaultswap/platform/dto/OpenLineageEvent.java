package com.creditdefaultswap.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * OpenLineage-compatible event structure.
 * Based on OpenLineage spec: https://openlineage.io/spec/
 */
public class OpenLineageEvent {

    @JsonProperty("eventType")
    private String eventType = "COMPLETE"; // START, COMPLETE, ABORT, FAIL

    @JsonProperty("eventTime")
    private ZonedDateTime eventTime;

    @JsonProperty("run")
    private Run run;

    @JsonProperty("job")
    private Job job;

    @JsonProperty("inputs")
    private List<Dataset> inputs;

    @JsonProperty("outputs")
    private List<Dataset> outputs;

    @JsonProperty("producer")
    private String producer = "credit-default-swap-platform/1.0";

    @JsonProperty("schemaURL")
    private String schemaURL = "https://openlineage.io/spec/1-0-5/OpenLineage.json";

    // Inner classes matching OpenLineage spec

    public static class Run {
        @JsonProperty("runId")
        private String runId;

        @JsonProperty("facets")
        private Map<String, Object> facets;

        public Run() {}

        public Run(String runId) {
            this.runId = runId;
        }

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public Map<String, Object> getFacets() {
            return facets;
        }

        public void setFacets(Map<String, Object> facets) {
            this.facets = facets;
        }
    }

    public static class Job {
        @JsonProperty("namespace")
        private String namespace = "credit-default-swap";

        @JsonProperty("name")
        private String name;

        @JsonProperty("facets")
        private Map<String, Object> facets;

        public Job() {}

        public Job(String name) {
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getFacets() {
            return facets;
        }

        public void setFacets(Map<String, Object> facets) {
            this.facets = facets;
        }
    }

    public static class Dataset {
        @JsonProperty("namespace")
        private String namespace = "postgres://cds_platform";

        @JsonProperty("name")
        private String name;

        @JsonProperty("facets")
        private Map<String, Object> facets;

        public Dataset() {}

        public Dataset(String name) {
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getFacets() {
            return facets;
        }

        public void setFacets(Map<String, Object> facets) {
            this.facets = facets;
        }
    }

    // Getters and setters

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public ZonedDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(ZonedDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public Run getRun() {
        return run;
    }

    public void setRun(Run run) {
        this.run = run;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public List<Dataset> getInputs() {
        return inputs;
    }

    public void setInputs(List<Dataset> inputs) {
        this.inputs = inputs;
    }

    public List<Dataset> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Dataset> outputs) {
        this.outputs = outputs;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getSchemaURL() {
        return schemaURL;
    }

    public void setSchemaURL(String schemaURL) {
        this.schemaURL = schemaURL;
    }
}
