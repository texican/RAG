#!/bin/bash

#################################################################################
# Cloud Pub/Sub Setup Script
# 
# This script provisions Google Cloud Pub/Sub topics and subscriptions for the
# RAG system, replacing the containerized Apache Kafka infrastructure.
#
# Prerequisites:
#   - GCP project created and configured (GCP-INFRA-001)
#   - gcloud CLI authenticated
#   - Pub/Sub API enabled
#
# Usage:
#   bash scripts/gcp/11-setup-pubsub.sh
#
# What this script does:
#   1. Enables Pub/Sub API
#   2. Creates 5 Pub/Sub topics for RAG messaging
#   3. Creates subscriptions for each consumer
#   4. Configures message retention and acknowledgment
#   5. Sets up dead letter topics
#   6. Configures IAM permissions for GKE
#   7. Outputs connection information
#
#################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
PROJECT_ID=${GCP_PROJECT_ID:-"byo-rag-dev"}
REGION=${GCP_REGION:-"us-central1"}

# Topic names (matching current Kafka topics)
TOPICS=(
    "document-processing"
    "embedding-generation"
    "rag-queries"
    "rag-responses"
    "feedback"
    "dead-letter-queue"
)

# Subscription configuration
declare -A SUBSCRIPTIONS
SUBSCRIPTIONS["document-processing"]="document-service-processor"
SUBSCRIPTIONS["embedding-generation"]="embedding-service-consumer"
SUBSCRIPTIONS["rag-queries"]="core-service-query-handler"
SUBSCRIPTIONS["rag-responses"]="external-response-consumer"
SUBSCRIPTIONS["feedback"]="core-service-feedback-handler"
SUBSCRIPTIONS["dead-letter-queue"]="dlq-monitor"

# Message retention (7 days to match Kafka)
MESSAGE_RETENTION_DURATION="7d"
ACK_DEADLINE="60s"  # 60 seconds to acknowledge messages

# Check if running in correct directory
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}Error: Must run from RAG project root directory${NC}"
    exit 1
fi

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Cloud Pub/Sub Setup${NC}"
echo -e "${BLUE}================================${NC}"
echo ""
echo "Configuration:"
echo "  Project ID: $PROJECT_ID"
echo "  Region: $REGION"
echo "  Topics: ${#TOPICS[@]}"
echo "  Subscriptions: ${#SUBSCRIPTIONS[@]}"
echo "  Message Retention: $MESSAGE_RETENTION_DURATION"
echo "  Ack Deadline: $ACK_DEADLINE"
echo ""

# Confirm with user
read -p "Continue with these settings? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Setup cancelled."
    exit 0
fi

#################################################################################
# Step 1: Check Prerequisites
#################################################################################

echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI not found. Please install it first.${NC}"
    exit 1
fi

# Check if authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo -e "${RED}Error: Not authenticated with gcloud. Run 'gcloud auth login'${NC}"
    exit 1
fi

# Set project
gcloud config set project "$PROJECT_ID" --quiet

# Check if Pub/Sub API is enabled
echo "Checking if Pub/Sub API is enabled..."
if ! gcloud services list --enabled --filter="config.name:pubsub.googleapis.com" --format="value(config.name)" | grep -q "pubsub.googleapis.com"; then
    echo "Enabling Pub/Sub API..."
    gcloud services enable pubsub.googleapis.com --project="$PROJECT_ID"
    echo "Waiting for API to be fully enabled (30 seconds)..."
    sleep 30
fi

echo -e "${GREEN}✓ Prerequisites check complete${NC}"
echo ""

#################################################################################
# Step 2: Create Topics
#################################################################################

echo -e "${YELLOW}Step 2: Creating Pub/Sub topics...${NC}"

for topic in "${TOPICS[@]}"; do
    echo "Creating topic: $topic"
    
    if gcloud pubsub topics describe "$topic" --project="$PROJECT_ID" &> /dev/null; then
        echo "  Topic '$topic' already exists, skipping..."
    else
        gcloud pubsub topics create "$topic" \
            --project="$PROJECT_ID" \
            --message-retention-duration="$MESSAGE_RETENTION_DURATION" \
            --labels="environment=production,service=rag" \
            --quiet
        
        echo -e "  ${GREEN}✓ Created topic: $topic${NC}"
    fi
done

echo -e "${GREEN}✓ All topics created${NC}"
echo ""

#################################################################################
# Step 3: Create Subscriptions
#################################################################################

echo -e "${YELLOW}Step 3: Creating subscriptions...${NC}"

for topic in "${!SUBSCRIPTIONS[@]}"; do
    subscription="${SUBSCRIPTIONS[$topic]}"
    
    echo "Creating subscription: $subscription → $topic"
    
    if gcloud pubsub subscriptions describe "$subscription" --project="$PROJECT_ID" &> /dev/null; then
        echo "  Subscription '$subscription' already exists, skipping..."
    else
        # Check if topic exists
        if ! gcloud pubsub topics describe "$topic" --project="$PROJECT_ID" &> /dev/null; then
            echo -e "  ${RED}Error: Topic '$topic' does not exist${NC}"
            continue
        fi
        
        # Create subscription with DLQ (except for DLQ subscription itself)
        if [ "$topic" != "dead-letter-queue" ]; then
            gcloud pubsub subscriptions create "$subscription" \
                --project="$PROJECT_ID" \
                --topic="$topic" \
                --ack-deadline="$ACK_DEADLINE" \
                --message-retention-duration="$MESSAGE_RETENTION_DURATION" \
                --dead-letter-topic="dead-letter-queue" \
                --max-delivery-attempts=5 \
                --labels="environment=production,service=rag" \
                --quiet
        else
            # DLQ subscription without DLQ (to avoid recursion)
            gcloud pubsub subscriptions create "$subscription" \
                --project="$PROJECT_ID" \
                --topic="$topic" \
                --ack-deadline="$ACK_DEADLINE" \
                --message-retention-duration="$MESSAGE_RETENTION_DURATION" \
                --labels="environment=production,service=rag,role=dead-letter" \
                --quiet
        fi
        
        echo -e "  ${GREEN}✓ Created subscription: $subscription${NC}"
    fi
done

echo -e "${GREEN}✓ All subscriptions created${NC}"
echo ""

#################################################################################
# Step 4: Configure IAM Permissions
#################################################################################

echo -e "${YELLOW}Step 4: Configuring IAM permissions...${NC}"

# Grant GKE service account permissions
GKE_SA="gke-node-sa@${PROJECT_ID}.iam.gserviceaccount.com"

if gcloud iam service-accounts describe "$GKE_SA" --project="$PROJECT_ID" &> /dev/null; then
    echo "Granting GKE service account Pub/Sub permissions..."
    
    # Publisher role (for producers)
    gcloud projects add-iam-policy-binding "$PROJECT_ID" \
        --member="serviceAccount:$GKE_SA" \
        --role="roles/pubsub.publisher" \
        --condition=None \
        --quiet
    
    # Subscriber role (for consumers)
    gcloud projects add-iam-policy-binding "$PROJECT_ID" \
        --member="serviceAccount:$GKE_SA" \
        --role="roles/pubsub.subscriber" \
        --condition=None \
        --quiet
    
    echo -e "${GREEN}✓ IAM permissions configured${NC}"
else
    echo -e "${YELLOW}Warning: GKE service account not found. Skipping IAM binding.${NC}"
    echo "You'll need to grant permissions manually when creating the GKE cluster."
fi

echo ""

#################################################################################
# Step 5: Create Monitoring Dashboard
#################################################################################

echo -e "${YELLOW}Step 5: Setting up monitoring...${NC}"

echo "Creating Cloud Monitoring alert policies..."

# Create alert for high message age (indicates consumer issues)
if ! gcloud alpha monitoring policies list --filter="displayName:'Pub/Sub High Message Age'" --project="$PROJECT_ID" --format="value(name)" | grep -q "policies"; then
    cat > /tmp/pubsub-alert-policy.yaml <<EOF
displayName: Pub/Sub High Message Age
conditions:
  - displayName: Message age > 5 minutes
    conditionThreshold:
      filter: resource.type="pubsub_subscription" AND metric.type="pubsub.googleapis.com/subscription/oldest_unacked_message_age"
      comparison: COMPARISON_GT
      thresholdValue: 300
      duration: 60s
      aggregations:
        - alignmentPeriod: 60s
          perSeriesAligner: ALIGN_MEAN
notificationChannels: []
alertStrategy:
  autoClose: 604800s
EOF
    
    gcloud alpha monitoring policies create --policy-from-file=/tmp/pubsub-alert-policy.yaml --project="$PROJECT_ID" --quiet || true
    rm /tmp/pubsub-alert-policy.yaml
fi

echo -e "${GREEN}✓ Monitoring configured${NC}"
echo ""

#################################################################################
# Step 6: Display Summary
#################################################################################

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Pub/Sub Topics Created:"
for topic in "${TOPICS[@]}"; do
    echo "  • $topic"
done
echo ""
echo "Subscriptions Created:"
for topic in "${!SUBSCRIPTIONS[@]}"; do
    subscription="${SUBSCRIPTIONS[$topic]}"
    echo "  • $subscription → $topic"
done
echo ""
echo "Configuration Details:"
echo "  Message Retention: $MESSAGE_RETENTION_DURATION"
echo "  Ack Deadline: $ACK_DEADLINE"
echo "  Max Delivery Attempts: 5"
echo "  Dead Letter Queue: dead-letter-queue"
echo ""
echo "IAM Permissions:"
echo "  • $GKE_SA"
echo "  • roles/pubsub.publisher (for producers)"
echo "  • roles/pubsub.subscriber (for consumers)"
echo ""
echo "Spring Boot Configuration:"
echo "  spring:"
echo "    cloud:"
echo "      gcp:"
echo "        pubsub:"
echo "          project-id: $PROJECT_ID"
echo ""
echo "Environment Variables:"
echo "  export GCP_PROJECT_ID=$PROJECT_ID"
echo "  export SPRING_CLOUD_GCP_PUBSUB_PROJECT_ID=$PROJECT_ID"
echo ""
echo "Verify Topics:"
echo "  gcloud pubsub topics list --project=$PROJECT_ID"
echo ""
echo "Verify Subscriptions:"
echo "  gcloud pubsub subscriptions list --project=$PROJECT_ID"
echo ""
echo "Test Message Publishing:"
echo "  gcloud pubsub topics publish document-processing \\"
echo "      --project=$PROJECT_ID \\"
echo "      --message='{\"documentId\":\"test-123\"}'"
echo ""
echo "Test Message Consumption:"
echo "  gcloud pubsub subscriptions pull document-service-processor \\"
echo "      --project=$PROJECT_ID \\"
echo "      --auto-ack \\"
echo "      --limit=1"
echo ""
echo "Management Console:"
echo "  https://console.cloud.google.com/cloudpubsub?project=$PROJECT_ID"
echo ""
echo "Monitoring Dashboard:"
echo "  https://console.cloud.google.com/monitoring/dashboards?project=$PROJECT_ID"
echo ""
echo "Cost Estimate:"
echo "  • Ingestion: ~\$0.06/GB"
echo "  • Delivery: ~\$0.09/GB"
echo "  • Estimated monthly: \$0.69-7.00 (based on 1-10M messages)"
echo "  • Free tier: 10 GB/month included"
echo ""
echo -e "${GREEN}Next Steps:${NC}"
echo "  1. Update service dependencies (Spring Cloud GCP Pub/Sub)"
echo "  2. Migrate producer code (KafkaTemplate → PubSubTemplate)"
echo "  3. Migrate consumer code (@KafkaListener → @PubSubListener)"
echo "  4. Update application.yml configuration"
echo "  5. Test message flows in development"
echo "  6. Deploy and validate in GKE"
echo "  7. Monitor message processing and latency"
echo ""
echo "See docs/deployment/KAFKA_TO_PUBSUB_MIGRATION.md for detailed migration guide"
echo ""
