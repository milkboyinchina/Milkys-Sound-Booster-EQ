#!/usr/bin/env bash
# ==============================================================================
# Setup Environment for Google Jules & Autonomous Runners
# Milkys Sound Booster & EQ
# ==============================================================================

set -e

echo "🤖 [Jules Setup] Initializing VM build environment for Milkys Sound Booster & EQ..."

# 1. Ensure .env configuration exists
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        echo "📄 [Jules Setup] Copying .env.example -> .env"
        cp .env.example .env
    else
        echo "⚠️ [Jules Setup] Warning: .env.example missing! Creating default .env"
        cat << 'EOF' > .env
APP_NAME=Milkys Sound Booster & EQ
APPLICATION_ID=com.milkys.soundbooster
VERSION_CODE=26072301
VERSION_NAME=0.1
BUILD_OUTPUT_DIR=.build-outputs
BUILD_LOGS_DIR=logs
GOOGLE_ADS_API_KEY=ca-app-pub-3940256099942544~3347511713
INCLUDE_GOOGLE_ADS=true
DEVELOPER_WEBSITE_URL=https://milkys.app
PRIVACY_POLICY_URL=https://milkys.app/privacy
EOF
    fi
fi

# 2. Make scripts and Gradle wrapper executable
echo "🔑 [Jules Setup] Granting execution permissions to scripts and gradlew..."
chmod +x scripts/*.sh gradlew 2>/dev/null || true

# 3. Create required logs and build outputs directories
mkdir -p logs .build-outputs

# 4. Verify system dependencies
echo "🔍 [Jules Setup] Validating system JDK and Android SDK requirements..."
if [ -f "scripts/check_requirements.sh" ]; then
    bash scripts/check_requirements.sh || {
        echo "⚠️ [Jules Setup] Requirement check reported warnings. Continuing with setup..."
    }
fi

# 5. Pre-warm Gradle wrapper
echo "⚙️ [Jules Setup] Verifying Gradle wrapper status..."
./gradlew --version

echo "✅ [Jules Setup] Environment initialization complete! Ready for Jules task execution."
