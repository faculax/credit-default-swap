.PHONY: help start stop restart status logs scan upload clean

COMPOSE_FILE := compliance/docker-compose.defectdojo.yml
BACKEND_DIR := backend
REPORTS_DIR := $(BACKEND_DIR)/target/security-reports

# Colors
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
CYAN := \033[0;36m
RESET := \033[0m

help: ## Show this help message
	@echo ""
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo "$(CYAN) DEFECTDOJO COMPLIANCE PLATFORM$(RESET)"
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo ""
	@echo "$(YELLOW)COMMANDS:$(RESET)"
	@echo ""
	@echo "  $(GREEN)make start$(RESET)       Start DefectDojo services"
	@echo "  $(GREEN)make stop$(RESET)        Stop DefectDojo services"
	@echo "  $(GREEN)make restart$(RESET)     Restart DefectDojo services"
	@echo "  $(GREEN)make status$(RESET)      Check service status"
	@echo "  $(GREEN)make logs$(RESET)        View service logs"
	@echo "  $(GREEN)make scan$(RESET)        Run security scans on backend"
	@echo "  $(GREEN)make upload$(RESET)      Upload scan results to DefectDojo"
	@echo "  $(GREEN)make clean$(RESET)       Remove all containers and volumes"
	@echo ""
	@echo "$(YELLOW)EXAMPLES:$(RESET)"
	@echo ""
	@echo "  $(BLUE)# Start DefectDojo$(RESET)"
	@echo "  make start"
	@echo ""
	@echo "  $(BLUE)# Run scans and upload results$(RESET)"
	@echo "  make scan"
	@echo "  make upload"
	@echo ""
	@echo "  $(BLUE)# View logs$(RESET)"
	@echo "  make logs"
	@echo ""
	@echo "$(YELLOW)ACCESS:$(RESET)"
	@echo ""
	@echo "  URL:      $(CYAN)http://localhost:8081$(RESET)"
	@echo "  Username: $(CYAN)admin$(RESET)"
	@echo "  Password: $(CYAN)admin$(RESET)"
	@echo ""
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo ""

start: ## Start DefectDojo services
	@echo ""
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo "$(CYAN) STARTING DEFECTDOJO$(RESET)"
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo ""
	@echo "$(BLUE)▶ Starting DefectDojo services...$(RESET)"
	@docker-compose -f $(COMPOSE_FILE) up -d
	@echo ""
	@echo "$(GREEN)✓ DefectDojo services started successfully$(RESET)"
	@echo ""
	@echo "$(BLUE)▶ Waiting for services to initialize...$(RESET)"
	@sleep 5
	@echo ""
	@echo "$(YELLOW)SERVICE STATUS:$(RESET)"
	@docker-compose -f $(COMPOSE_FILE) ps
	@echo ""
	@echo "$(GREEN)ACCESS DEFECTDOJO:$(RESET)"
	@echo "  URL:      $(CYAN)http://localhost:8081$(RESET)"
	@echo "  Username: $(CYAN)admin$(RESET)"
	@echo "  Password: $(CYAN)admin$(RESET)"
	@echo ""
	@echo "$(YELLOW)⚠ Note: First startup may take 2-3 minutes for database initialization$(RESET)"
	@echo ""

stop: ## Stop DefectDojo services
	@echo ""
	@echo "$(BLUE)▶ Stopping DefectDojo services...$(RESET)"
	@docker-compose -f $(COMPOSE_FILE) down
	@echo "$(GREEN)✓ DefectDojo services stopped$(RESET)"

restart: stop start ## Restart DefectDojo services

status: ## Check DefectDojo service status
	@echo ""
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo "$(CYAN) DEFECTDOJO STATUS$(RESET)"
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo ""
	@docker-compose -f $(COMPOSE_FILE) ps
	@echo ""
	@echo "$(BLUE)▶ Testing DefectDojo availability...$(RESET)"
	@if curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/login | grep -q "200"; then \
		echo "$(GREEN)✓ DefectDojo is accessible at http://localhost:8081$(RESET)"; \
	else \
		echo "$(YELLOW)⚠ DefectDojo may not be fully initialized yet$(RESET)"; \
		echo "$(BLUE)▶ Run: make logs$(RESET)"; \
	fi
	@echo ""

logs: ## View DefectDojo logs
	@echo ""
	@echo "$(BLUE)▶ Showing DefectDojo logs (Ctrl+C to exit)...$(RESET)"
	@echo ""
	@docker-compose -f $(COMPOSE_FILE) logs -f

scan: ## Run security scans on backend
	@echo ""
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo "$(CYAN) SECURITY SCANNING$(RESET)"
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo ""
	@echo "$(BLUE)▶ Creating reports directory...$(RESET)"
	@mkdir -p $(REPORTS_DIR)
	@echo ""
	@echo "$(BLUE)▶ Running OWASP Dependency Check...$(RESET)"
	@cd $(BACKEND_DIR) && mvn org.owasp:dependency-check-maven:check \
		-DdataDirectory=./dependency-check-data \
		-Dformat=JSON \
		-DprettyPrint=true
	@if [ -f "$(BACKEND_DIR)/target/dependency-check-report.json" ]; then \
		cp $(BACKEND_DIR)/target/dependency-check-report.json $(REPORTS_DIR)/; \
		echo "$(GREEN)✓ OWASP Dependency Check complete$(RESET)"; \
	fi
	@echo ""
	@echo "$(BLUE)▶ Running SpotBugs security analysis...$(RESET)"
	@cd $(BACKEND_DIR) && mvn compile spotbugs:spotbugs \
		-Dspotbugs.xmlOutput=true \
		-Dspotbugs.xmlOutputDirectory=target/security-reports
	@echo "$(GREEN)✓ SpotBugs analysis complete$(RESET)"
	@echo ""
	@echo "$(BLUE)▶ Running Checkstyle...$(RESET)"
	@cd $(BACKEND_DIR) && mvn checkstyle:checkstyle
	@if [ -f "$(BACKEND_DIR)/target/checkstyle-result.xml" ]; then \
		cp $(BACKEND_DIR)/target/checkstyle-result.xml $(REPORTS_DIR)/; \
		echo "$(GREEN)✓ Checkstyle complete$(RESET)"; \
	fi
	@echo ""
	@echo "$(BLUE)▶ Running PMD analysis...$(RESET)"
	@cd $(BACKEND_DIR) && mvn pmd:pmd pmd:cpd
	@if [ -f "$(BACKEND_DIR)/target/pmd.xml" ]; then \
		cp $(BACKEND_DIR)/target/pmd.xml $(REPORTS_DIR)/; \
		echo "$(GREEN)✓ PMD analysis complete$(RESET)"; \
	fi
	@echo ""
	@echo "$(GREEN)✓ All security scans completed!$(RESET)"
	@echo ""
	@echo "$(BLUE)▶ Results saved to: $(REPORTS_DIR)/$(RESET)"
	@echo ""
	@echo "$(BLUE)▶ Next step: Run 'make upload' to upload results to DefectDojo$(RESET)"
	@echo ""

upload: ## Upload scan results to DefectDojo
	@echo ""
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo "$(CYAN) UPLOADING TO DEFECTDOJO$(RESET)"
	@echo "$(CYAN)═══════════════════════════════════════════════════════════════$(RESET)"
	@echo ""
	@bash compliance/scripts/upload-to-defectdojo.sh

clean: ## Remove all DefectDojo containers and volumes
	@echo ""
	@echo "$(YELLOW)⚠ WARNING: This will remove all DefectDojo containers, volumes, and data!$(RESET)"
	@echo ""
	@read -p "Are you sure? Type 'yes' to confirm: " confirm; \
	if [ "$$confirm" = "yes" ]; then \
		echo "$(BLUE)▶ Removing DefectDojo containers and volumes...$(RESET)"; \
		docker-compose -f $(COMPOSE_FILE) down -v; \
		echo "$(GREEN)✓ DefectDojo cleaned up successfully$(RESET)"; \
		echo "$(BLUE)▶ All data has been removed. Run 'make start' to recreate.$(RESET)"; \
	else \
		echo "$(BLUE)▶ Cleanup cancelled$(RESET)"; \
	fi
