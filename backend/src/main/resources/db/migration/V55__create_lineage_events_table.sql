-- Story 06: Data Lineage
-- Create lineage_events table to track dataset lineage, transformations, and audit trails

CREATE TABLE lineage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset VARCHAR(255) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    inputs JSONB,
    outputs JSONB,
    user_name VARCHAR(100),
    run_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_lineage_events_dataset ON lineage_events(dataset);
CREATE INDEX idx_lineage_events_created_at ON lineage_events(created_at DESC);
CREATE INDEX idx_lineage_events_run_id ON lineage_events(run_id);

COMMENT ON TABLE lineage_events IS 'Audit log for data lineage and transformations';
COMMENT ON COLUMN lineage_events.dataset IS 'Name or identifier of the dataset';
COMMENT ON COLUMN lineage_events.operation IS 'Type of operation (e.g. MIGRATION, TRANSFORM, INGEST)';
COMMENT ON COLUMN lineage_events.inputs IS 'JSON array of input dataset/file identifiers';
COMMENT ON COLUMN lineage_events.outputs IS 'JSON array of output dataset/file identifiers';
COMMENT ON COLUMN lineage_events.run_id IS 'Identifier for the batch/run that produced this event';
