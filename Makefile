# ==============================================================================
# Makefile - Milkys Sound Booster & EQ Automation Commands
# ==============================================================================

.PHONY: all help build debug release playstore fdroid both check test lint clean setup

# Default target
all: help

## help: Display available Makefile commands
help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Available Targets:"
	@echo "  build       - Run standard build script (default debug task)"
	@echo "  debug       - Build Debug APK"
	@echo "  release     - Build Release APK"
	@echo "  playstore   - Build Google Play Console Release Bundle (.aab) & APK"
	@echo "  fdroid      - Build F-Droid distribution package (No AdMob ads)"
	@echo "  both        - Build both Play Store and F-Droid release packages"
	@echo "  check       - Run environment & toolchain pre-checks"
	@echo "  test        - Run JVM unit & Compose snapshot tests"
	@echo "  lint        - Run Android Lint static code analysis"
	@echo "  clean       - Clean Gradle build outputs and caches"
	@echo "  setup       - Initialize .env and permissions for automation scripts"

## build: Run standard build script
build:
	bash scripts/build.sh assembleDebug

## debug: Build Debug APK
debug:
	bash scripts/build.sh assembleDebug

## release: Build Release APK
release:
	bash scripts/build.sh assembleRelease

## playstore: Build Google Play Console Release Bundle (.aab) & APK
playstore:
	bash scripts/build_play_console_release.sh

## fdroid: Build F-Droid distribution package
fdroid:
	BUILD_TARGET=fdroid bash scripts/build.sh assembleRelease

## both: Build both Play Store and F-Droid release packages
both:
	BUILD_TARGET=both bash scripts/build.sh assembleRelease

## check: Run environment pre-checks
check:
	bash scripts/check_requirements.sh

## test: Run unit & Compose tests
test:
	./gradlew testDebugUnitTest

## lint: Run Android Lint analysis
lint:
	./gradlew lintDebug

## clean: Clean build outputs
clean:
	./gradlew clean

## setup: Initialize configuration files and script permissions
setup:
	@if [ ! -f .env ] && [ -f .env.example ]; then cp .env.example .env; fi
	chmod +x scripts/*.sh
