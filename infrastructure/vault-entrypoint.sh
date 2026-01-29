set -e

echo "=== Starting Vault ==="

if curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; then
    echo "Vault is already running"
    tail -f /dev/null
    exit 0
fi

vault server -dev \
  -dev-root-token-id=root \
  -dev-listen-address=0.0.0.0:8200 &

echo "Waiting for Vault to start..."
sleep 5

export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='root'

if ! vault secrets list 2>/dev/null | grep -q "secret/"; then
    echo "Enabling KV engine..."
    vault secrets enable -path=secret kv
fi

SECRETS_DIR="/vault/config/secrets"
if [ -d "$SECRETS_DIR" ]; then
    echo "Loading secrets from $SECRETS_DIR"
    for secret_file in "$SECRETS_DIR"/*.json; do
        if [ -f "$secret_file" ]; then
            secret_name=$(basename "$secret_file" .json)
            echo "📦 Loading: $secret_name"
            vault kv put "secret/$secret_name" @"$secret_file"
        fi
    done
fi

echo ""
echo "========================================"
echo "✅ Vault is ready!"
echo "🌐 UI: http://localhost:8200"
echo "🔑 Token: root"
echo "========================================"

wait