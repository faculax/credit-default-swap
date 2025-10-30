# Traceability Matrix

Maps original user stories to functional test story documents & scenario ID ranges.

| Epic | Original Story File | Functional Test Story Doc | Scenario ID Range | Tags (Primary) |
|------|---------------------|---------------------------|-------------------|----------------|
| 03 | story_3_1_cds_trade_capture_ui.md | test_story_3_1_cds_trade_capture_ui.md | FT-3-1-001..018 | @TRADE @CRITPATH |
| 03 | story_3_2_cds_validation_rules.md | test_story_3_2_cds_validation_rules.md | FT-3-2-001..024 | @TRADE @NEGATIVE |
| 03 | story_3_3_cds_trade_persist.md | test_story_3_3_cds_trade_persist.md | FT-3-3-001..016 | @TRADE @PERSISTENCE |
| 03 | story_3_4_cds_booking_confirmation.md | test_story_3_4_cds_booking_confirmation.md | FT-3-4-001..014 | @TRADE @CONFIRMATION |
| 03 | story_3_5_cds_auth_security.md | test_story_3_5_cds_auth_security.md | FT-3-5-001..020 | @TRADE @SECURITY |
| 04 | story_4_1_record_credit_event.md | test_story_4_1_record_credit_event.md | FT-4-1-001..022 | @CREDIT_EVENT @CRITPATH |
| 04 | story_4_2_validate_and_persist_event.md | test_story_4_2_validate_and_persist_event.md | FT-4-2-001..018 | @CREDIT_EVENT @NEGATIVE |
| 04 | story_4_3_cash_settlement_calculation.md | test_story_4_3_cash_settlement_calculation.md | FT-4-3-001..020 | @CREDIT_EVENT @CALCULATION |
| 04 | story_4_4_physical_settlement_scaffold.md | test_story_4_4_physical_settlement_scaffold.md | FT-4-4-001..012 | @CREDIT_EVENT @PHYSICAL |
| 04 | story_4_5_settlement_instructions_persistence.md | test_story_4_5_settlement_instructions_persistence.md | FT-4-5-001..015 | @CREDIT_EVENT @PERSISTENCE |
| 04 | story_4_6_audit_and_error_handling.md | test_story_4_6_audit_and_error_handling.md | FT-4-6-001..019 | @CREDIT_EVENT @AUDIT |
| 05 | story_5_1_schedule_generate_imm_coupon_events.md | test_story_5_1_schedule_generate_imm_coupon_events.md | FT-5-1-001..017 | @LIFECYCLE @SCHEDULER |
| 05 | story_5_2_accrual_net_cash_posting_engine.md | test_story_5_2_accrual_net_cash_posting_engine.md | FT-5-2-001..022 | @LIFECYCLE @ACCRUAL |
| 05 | story_5_3_economic_amend_workflow.md | test_story_5_3_economic_amend_workflow.md | FT-5-3-001..020 | @LIFECYCLE @WORKFLOW |
| 05 | story_5_4_notional_adjustment_termination_logic.md | test_story_5_4_notional_adjustment_termination_logic.md | FT-5-4-001..018 | @LIFECYCLE @NOTIONAL |
| 05 | story_5_5_novation_party_role_transition.md | test_story_5_5_novation_party_role_transition.md | FT-5-5-001..016 | @LIFECYCLE @NOVATION |
| 05 | story_5_6_compression_proposal_ingestion_execution.md | test_story_5_6_compression_proposal_ingestion_execution.md | FT-5-6-001..021 | @LIFECYCLE @COMPRESSION |
| 06 | README.md | test_story_6_index_and_constituent_management.md | FT-6-0-001..015 | @INDEX @MASTERING |
| 07 | story_7_1_isda-standard-model-integration-parity-tests.md | test_story_7_1_isda_standard_model_parity.md | FT-7-1-001..028 | @RISK @PARITY |
| 07 | story_7_2_core-risk-measures-engine.md | test_story_7_2_core_risk_measures.md | FT-7-2-001..030 | @RISK @MEASURES |
| 07 | story_7_3_curve-bucket-scenario-shock-module.md | test_story_7_3_curve_bucket_scenario_shock.md | FT-7-3-001..024 | @RISK @SCENARIO |
| 07 | story_7_4_benchmark-regression-harness.md | test_story_7_4_benchmark_regression_harness.md | FT-7-4-001..020 | @RISK @REGRESSION |
| 07 | story_7_5_ore_process_supervisor_and_adapter.md | test_story_7_5_ore_process_supervisor_adapter.md | FT-7-5-001..022 | @RISK @PROCESS |
| 07 | story_7_6_batched_scenarios_and_bucket_cs01.md | test_story_7_6_batched_scenarios_bucket_cs01.md | FT-7-6-001..026 | @RISK @SCALING |
| 08 | story_8_1_ccp_novation_account_enrichment.md | test_story_8_1_ccp_novation_account_enrichment.md | FT-8-1-001..017 | @MARGIN @NOVATION |
| 08 | story_8_2_daily_vm_im_statement_ingestion.md | test_story_8_2_daily_vm_im_statement_ingestion.md | FT-8-2-001..020 | @MARGIN @INGESTION |
| 08 | story_8_3_simm_sensitivities_im_calculator.md | test_story_8_3_simm_sensitivities_im_calculator.md | FT-8-3-001..028 | @MARGIN @SIMM |
| 08 | story_8_4_sa_ccr_exposure_engine.md | test_story_8_4_sa_ccr_exposure_engine.md | FT-8-4-001..022 | @MARGIN @EXPOSURE |
| 08 | story_8_5_margin_exposure_reconciliation_dashboard.md | test_story_8_5_margin_exposure_reconciliation_dashboard.md | FT-8-5-001..018 | @MARGIN @DASHBOARD |
| 09 | README.md | test_story_9_reference_market_data_mastering.md | FT-9-0-001..020 | @REFDATA @MASTERING |
| 10 | README.md | test_story_10_reporting_audit_replay.md | FT-10-0-001..024 | @REPORTING @AUDIT |
| 11 | README.md | test_story_11_single_name_cds_tech_debt.md | FT-11-0-001..016 | @TECH_DEBT @REFactor |
| 12 | README.md | test_story_12_cds_portfolio_aggregation.md | FT-12-0-001..022 | @PORTFOLIO @AGGREGATION |
| 13 | story_13_1_run-correlated-simulation.md | test_story_13_1_run_correlated_simulation.md | FT-13-1-001..030 | @SIMULATION @CRITPATH |
| 13 | story_13_2_view-run-progress.md | test_story_13_2_view_run_progress.md | FT-13-2-001..020 | @SIMULATION @UI |
| 13 | story_13_3_retrieve-portfolio-metrics.md | test_story_13_3_retrieve_portfolio_metrics.md | FT-13-3-001..022 | @SIMULATION @METRICS |
| 13 | story_13_4_diversification_benefit.md | test_story_13_4_diversification_benefit.md | FT-13-4-001..018 | @SIMULATION @DIVERSIFICATION |
| 13 | story_13_5_contributors_table.md | test_story_13_5_contributors_table.md | FT-13-5-001..018 | @SIMULATION @UI |
| 13 | story_13_6_deterministic_recovery_support.md | test_story_13_6_deterministic_recovery_support.md | FT-13-6-001..016 | @SIMULATION @RECOVERY |
| 13 | story_13_7_json_download.md | test_story_13_7_json_download.md | FT-13-7-001..012 | @SIMULATION @EXPORT |
| 13 | story_13_8_reproducibility_via_seed.md | test_story_13_8_reproducibility_via_seed.md | FT-13-8-001..014 | @SIMULATION @DETERMINISM |
| 13 | story_13_9_input_validation.md | test_story_13_9_input_validation.md | FT-13-9-001..020 | @SIMULATION @NEGATIVE |
| 13 | story_13_10_performance-baseline.md | test_story_13_10_performance_baseline.md | FT-13-10-001..016 | @SIMULATION @PERFORMANCE |
| 13 | story_13_11_metrics-glossary-modal.md | test_story_13_11_metrics_glossary_modal.md | FT-13-11-001..010 | @SIMULATION @UI |
| 13 | story_13_12_cancel-running-simulation.md | test_story_13_12_cancel_running_simulation.md | FT-13-12-001..018 | @SIMULATION @CANCELLATION |
| 13 | story_13_13_enable-stochastic-recovery.md | test_story_13_13_enable_stochastic_recovery.md | FT-13-13-001..018 | @SIMULATION @RECOVERY |
| 13 | story_13_14_configure-recovery-distribution.md | test_story_13_14_configure_recovery_distribution.md | FT-13-14-001..018 | @SIMULATION @RECOVERY |
| 13 | story_13_15_factor-recovery-correlation.md | test_story_13_15_factor_recovery_correlation.md | FT-13-15-001..020 | @SIMULATION @CORRELATION |
| 13 | story_13_16_extended-contributors-recovery-stats.md | test_story_13_16_extended_contributors_recovery_stats.md | FT-13-16-001..014 | @SIMULATION @UI |
| 13 | story_13_17_error-handling.md | test_story_13_17_error_handling.md | FT-13-17-001..018 | @SIMULATION @ERROR |
| 13 | story_13_18_audit-trail.md | test_story_13_18_audit_trail.md | FT-13-18-001..012 | @SIMULATION @AUDIT |
| 13 | story_13_19_security-resource-limits.md | test_story_13_19_security_resource_limits.md | FT-13-19-001..016 | @SIMULATION @SECURITY |
| 13 | story_13_20_determinism-test-harness.md | test_story_13_20_determinism_test_harness.md | FT-13-20-001..014 | @SIMULATION @DETERMINISM |
| 13 | story_13_21_monotonicity-consistency-checks.md | test_story_13_21_monotonicity_consistency_checks.md | FT-13-21-001..016 | @SIMULATION @CONSISTENCY |
| 13 | story_13_22_observability-metrics.md | test_story_13_22_observability_metrics.md | FT-13-22-001..015 | @SIMULATION @OBSERVABILITY |
| 13 | story_13_23_ui-parameter-persistence.md | test_story_13_23_ui_parameter_persistence.md | FT-13-23-001..012 | @SIMULATION @UI |
| 13 | story_13_24_accessibility-responsiveness.md | test_story_13_24_accessibility_responsiveness.md | FT-13-24-001..012 | @SIMULATION @ACCESSIBILITY |
| 13 | story_13_25_documentation_glossary_sync.md | test_story_13_25_documentation_glossary_sync.md | FT-13-25-001..010 | @SIMULATION @DOCS |
| 13 | story_13_26_cancellation_robustness.md | test_story_13_26_cancellation_robustness.md | FT-13-26-001..014 | @SIMULATION @RESILIENCE |
| 13 | story_13_27_scaling_to_multi_portfolio_future.md | test_story_13_27_scaling_to_multi_portfolio_future.md | FT-13-27-001..012 | @SIMULATION @SCALING |
| 14 (corp) | PHASE_2_3_MULTI_ASSET_PORTFOLIOS.md | test_story_14_corporate_bonds_phase_multi_asset_portfolios.md | FT-14C-0-001..020 | @BONDS @PORTFOLIO |
| 16 (credit) | story_16_1_db_migration_bonds_table.md | test_story_16_1_db_migration_bonds_table.md | FT-16-1-001..012 | @BONDS @DB |
| 16 (credit) | story_16_2_bond_domain_entity_repository_mapping.md | test_story_16_2_bond_domain_entity_repository_mapping.md | FT-16-2-001..014 | @BONDS @DOMAIN |
| 16 (credit) | story_16_3_bond_validation_layer.md | test_story_16_3_bond_validation_layer.md | FT-16-3-001..016 | @BONDS @VALIDATION |
| 16 (credit) | story_16_4_cashflow_schedule_day_count_utilities.md | test_story_16_4_cashflow_schedule_day_count_utilities.md | FT-16-4-001..018 | @BONDS @CASHFLOW |
| 16 (credit) | story_16_5_deterministic_bond_pricing_accrual.md | test_story_16_5_deterministic_bond_pricing_accrual.md | FT-16-5-001..018 | @BONDS @PRICING |
| 16 (credit) | story_16_6_yield_z_spread_solvers.md | test_story_16_6_yield_z_spread_solvers.md | FT-16-6-001..020 | @BONDS @SOLVER |
| 16 (credit) | story_16_7_survival_based_hazard_pricing_extension.md | test_story_16_7_survival_based_hazard_pricing_extension.md | FT-16-7-001..016 | @BONDS @HAZARD |
| 16 (credit) | story_16_8_bond_sensitivities_ir_dv01_spread_dv01_jtd.md | test_story_16_8_bond_sensitivities_ir_dv01_spread_dv01_jtd.md | FT-16-8-001..018 | @BONDS @SENSITIVITY |
| 16 (credit) | story_16_9_bond_crud_rest_endpoints.md | test_story_16_9_bond_crud_rest_endpoints.md | FT-16-9-001..020 | @BONDS @CRUD |
| 16 (credit) | story_16_10_bond_pricing_endpoint.md | test_story_16_10_bond_pricing_endpoint.md | FT-16-10-001..016 | @BONDS @PRICING |
| 16 (credit) | story_16_11_portfolio_aggregation_integration_bonds.md | test_story_16_11_portfolio_aggregation_integration_bonds.md | FT-16-11-001..016 | @BONDS @AGGREGATION |
| 16 (credit) | story_16_12_frontend_bond_creation_detail_view.md | test_story_16_12_frontend_bond_creation_detail_view.md | FT-16-12-001..018 | @BONDS @UI |
| 16 (credit) | story_16_13_frontend_portfolio_bond_metrics_columns.md | test_story_16_13_frontend_portfolio_bond_metrics_columns.md | FT-16-13-001..014 | @BONDS @UI |
| 16 (credit) | story_16_14_bond_testing_suite_unit_integration.md | test_story_16_14_bond_testing_suite_unit_integration.md | FT-16-14-001..012 | @BONDS @TESTING |
| 16 (credit) | story_16_15_performance_batch_pricing_preparation.md | test_story_16_15_performance_batch_pricing_preparation.md | FT-16-15-001..012 | @BONDS @PERFORMANCE |
| 16 (credit) | story_16_16_documentation_agent_guide_update.md | test_story_16_16_documentation_agent_guide_update.md | FT-16-16-001..010 | @BONDS @DOCS |
| 15 | story_15_1_basket_domain_model_persistence.md | test_story_15_1_basket_domain_model_persistence.md | FT-15-1-001..016 | @BASKET @DOMAIN |
| 15 | story_15_2_create_view_first_to_default_basket.md | test_story_15_2_create_view_first_to_default_basket.md | FT-15-2-001..020 | @BASKET @UI |
| 15 | story_15_3_first_to_default_pricing_fair_spread_solver.md | test_story_15_3_first_to_default_pricing_fair_spread_solver.md | FT-15-3-001..020 | @BASKET @PRICING |
| 15 | story_15_4_pricing_api_error_handling_validation.md | test_story_15_4_pricing_api_error_handling_validation.md | FT-15-4-001..018 | @BASKET @ERROR |
| 15 | story_15_5_convergence_diagnostics_exposure.md | test_story_15_5_convergence_diagnostics_exposure.md | FT-15-5-001..016 | @BASKET @DIAGNOSTICS |
| 15 | story_15_6_basket_builder_ui_phase_a.md | test_story_15_6_basket_builder_ui_phase_a.md | FT-15-6-001..018 | @BASKET @UI |
| 15 | story_15_7_basket_detail_valuation_panel.md | test_story_15_7_basket_detail_valuation_panel.md | FT-15-7-001..018 | @BASKET @UI |
| 15 | story_15_8_n-th_to_default_extension.md | test_story_15_8_n_th_to_default_extension.md | FT-15-8-001..018 | @BASKET @EXTENSION |
| 15 | story_15_9_correlation_recovery_sensitivities_aggregate.md | test_story_15_9_correlation_recovery_sensitivities_aggregate.md | FT-15-9-001..016 | @BASKET @SENSITIVITY |
| 15 | story_15_10_batch_pricing_path_reuse.md | test_story_15_10_batch_pricing_path_reuse.md | FT-15-10-001..016 | @BASKET @PERFORMANCE |
| 15 | story_15_11_portfolio_aggregation_integration.md | test_story_15_11_portfolio_aggregation_integration.md | FT-15-11-001..016 | @BASKET @AGGREGATION |
| 15 | story_15_12_tranche_attach_detach_groundwork.md | test_story_15_12_tranche_attach_detach_groundwork.md | FT-15-12-001..014 | @BASKET @TRANCHE |
| 15 | story_15_13_tranche_premium_loss_approximation.md | test_story_15_13_tranche_premium_loss_approximation.md | FT-15-13-001..018 | @BASKET @TRANCHE |
| 15 | story_15_14_performance_baseline_benchmark.md | test_story_15_14_performance_baseline_benchmark.md | FT-15-14-001..014 | @BASKET @PERFORMANCE |
| 15 | story_15_15_documentation_diagnostics.md | test_story_15_15_documentation_diagnostics.md | FT-15-15-001..010 | @BASKET @DOCS |
| 15 | story_15_16_error_validation_consistency.md | test_story_15_16_error_validation_consistency.md | FT-15-16-001..014 | @BASKET @ERROR |
| 15 | story_15_17_ui_enhancements_refresh_cycle.md | test_story_15_17_ui_enhancements_refresh_cycle.md | FT-15-17-001..012 | @BASKET @UI |
| 15 | story_15_18_portfolio_view_integration_ui.md | test_story_15_18_portfolio_view_integration_ui.md | FT-15-18-001..014 | @BASKET @UI |
| 15 | story_15_19_sensitivities_toggle_display.md | test_story_15_19_sensitivities_toggle_display.md | FT-15-19-001..012 | @BASKET @UI |
| 15 | story_15_20_correlation_bump_configuration.md | test_story_15_20_correlation_bump_configuration.md | FT-15-20-001..014 | @BASKET @CONFIG |
| 15 | story_15_21_recovery_override_handling.md | test_story_15_21_recovery_override_handling.md | FT-15-21-001..014 | @BASKET @RECOVERY |
| 15 | story_15_22_deterministic_vs_random_seed_control.md | test_story_15_22_deterministic_vs_random_seed_control.md | FT-15-22-001..012 | @BASKET @DETERMINISM |
| 15 | story_15_23_weight_rebalancing_after_validation.md | test_story_15_23_weight_rebalancing_after_validation.md | FT-15-23-001..012 | @BASKET @WEIGHTING |
| 15 | story_15_24_tranche_interval_validation_errors.md | test_story_15_24_tranche_interval_validation_errors.md | FT-15-24-001..012 | @BASKET @VALIDATION |
| 15 | story_15_25_batch_pricing_ui_phase_b.md | test_story_15_25_batch_pricing_ui_phase_b.md | FT-15-25-001..012 | @BASKET @UI |
