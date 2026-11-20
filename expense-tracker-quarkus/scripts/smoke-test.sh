#!/usr/bin/env bash
# End-to-end check against a running stack (docker compose up). Everything goes through the gateway.
# Usage: ./scripts/smoke-test.sh [gateway-url]
set -euo pipefail
GW="${1:-http://localhost:8080}"
RUN="$(date +%s)"
USER1="smoke$RUN"; USER2="other$RUN"
PASS=0; FAIL=0
BODY="$(mktemp)"; trap 'rm -f "$BODY"' EXIT

check() { # name expected actual
  if [ "$2" = "$3" ]; then echo "  PASS  $1"; PASS=$((PASS+1)); else echo "  FAIL  $1 (expected $2, got $3) body: $(head -c 300 "$BODY")"; FAIL=$((FAIL+1)); fi
}
code() { curl -s -o "$BODY" -w '%{http_code}' "$@"; }
json() { python3 -c "import sys,json; print(json.load(open(sys.argv[1]))$1)" "$BODY"; }
CT=(-H 'Content-Type: application/json')
TODAY="$(date +%Y-%m-%d)"

echo "Gateway: $GW"
# Request bodies are built up front: bash 3.2 (macOS) mis-parses escaped quotes inside "$(...)".
REG="{\"username\":\"$USER1\",\"email\":\"$USER1@example.com\",\"password\":\"S3cure!pass\"}"
LOGIN="{\"username\":\"$USER1\",\"password\":\"S3cure!pass\"}"
BAD_LOGIN="{\"username\":\"$USER1\",\"password\":\"wrong-pass\"}"
REG2="{\"username\":\"$USER2\",\"email\":\"$USER2@example.com\",\"password\":\"S3cure!pass\"}"
LOGIN2="{\"username\":\"$USER2\",\"password\":\"S3cure!pass\"}"
GROCERIES='{"title":"Weekly groceries","amount":82.4,"category":"FOOD","expenseDate":"2026-11-03"}'
BUS_PASS="{\"title\":\"Bus pass\",\"amount\":15,\"category\":\"TRAVEL\",\"expenseDate\":\"$TODAY\"}"
UPDATED='{"title":"Monthly groceries","amount":310,"category":"FOOD","expenseDate":"2026-11-03"}'
INVALID='{"title":"Bad","amount":-5,"category":"PETS","expenseDate":"2999-01-01"}'
check "register (public) -> 201" 201 "$(code -X POST "$GW/api/auth/register" "${CT[@]}" -d "$REG")"
check "register duplicate -> 409" 409 "$(code -X POST "$GW/api/auth/register" "${CT[@]}" -d "$REG")"
check "duplicate error code" USER_ALREADY_EXISTS "$(json "['code']")"
check "login wrong password -> 401" 401 "$(code -X POST "$GW/api/auth/login" "${CT[@]}" -d "$BAD_LOGIN")"
check "login -> 200" 200 "$(code -X POST "$GW/api/auth/login" "${CT[@]}" -d "$LOGIN")"
TOKEN=$(json "['token']")
AUTH=(-H "Authorization: Bearer $TOKEN")

check "no JWT -> 401" 401 "$(code "$GW/api/expenses")"
check "invalid JWT -> 401" 401 "$(code "$GW/api/expenses" -H 'Authorization: Bearer abc.def.ghi')"

check "create -> 201" 201 "$(code -X POST "$GW/api/expenses" "${AUTH[@]}" "${CT[@]}" -d "$GROCERIES")"
ID=$(json "['id']")
check "created category" FOOD "$(json "['category']")"
LOCATION=$(curl -s -D - -o /dev/null -X POST "$GW/api/expenses" "${AUTH[@]}" "${CT[@]}" \
  -d "$BUS_PASS" \
  | tr -d '\r' | awk -F': ' 'tolower($1)=="location"{print $2}')
check "Location header points at gateway" "$GW/api/expenses" "${LOCATION%/*}"
check "list -> 200" 200 "$(code "$GW/api/expenses?page=0&size=10" "${AUTH[@]}")"
check "list count" 2 "$(json ".__len__()")"
check "filter by category" 1 "$(code "$GW/api/expenses?category=TRAVEL" "${AUTH[@]}" >/dev/null; json ".__len__()")"
check "get by id -> 200" 200 "$(code "$GW/api/expenses/$ID" "${AUTH[@]}")"
check "update -> 200" 200 "$(code -X PUT "$GW/api/expenses/$ID" "${AUTH[@]}" "${CT[@]}" -d "$UPDATED")"
check "updated title" "Monthly groceries" "$(json "['title']")"
check "summary -> 200" 200 "$(code "$GW/api/expenses/summary" "${AUTH[@]}")"
check "summary FOOD total" 310.0 "$(json "['FOOD']")"
check "xml export -> 200" 200 "$(code "$GW/api/expenses/export/xml" "${AUTH[@]}")"
check "xml has mapped record" 1 "$(grep -c '<Description>MONTHLY GROCERIES</Description><Amt>310.00</Amt><CatCode>F01</CatCode><TxnDate>03-11-2026</TxnDate>' "$BODY" || true)"
check "invalid body -> 400" 400 "$(code -X POST "$GW/api/expenses" "${AUTH[@]}" "${CT[@]}" -d "$INVALID")"
check "validation error code" VALIDATION_FAILED "$(json "['code']")"
check "unknown id -> 404" 404 "$(code "$GW/api/expenses/999999" "${AUTH[@]}")"
check "not found error code" EXPENSE_NOT_FOUND "$(json "['code']")"
check "delete -> 204" 204 "$(code -X DELETE "$GW/api/expenses/$ID" "${AUTH[@]}")"
check "deleted -> 404" 404 "$(code "$GW/api/expenses/$ID" "${AUTH[@]}")"

# A second user must not see the first user's data
code -X POST "$GW/api/auth/register" "${CT[@]}" -d "$REG2" >/dev/null
code -X POST "$GW/api/auth/login" "${CT[@]}" -d "$LOGIN2" >/dev/null
TOKEN2=$(json "['token']")
check "other user sees 0 expenses" 0 "$(code "$GW/api/expenses" -H "Authorization: Bearer $TOKEN2" >/dev/null; json ".__len__()")"

check "gateway metrics -> 200" 200 "$(code "$GW/q/metrics")"

echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
