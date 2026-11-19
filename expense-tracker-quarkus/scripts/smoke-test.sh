#!/usr/bin/env bash
# End-to-end check against a running stack (docker compose up). Everything goes through the gateway.
# Usage: ./scripts/smoke-test.sh [gateway-url]
set -euo pipefail
GW="${1:-http://localhost:8082}"
EMAIL="smoke.$(date +%s)@example.com"
PASS=0; FAIL=0
BODY="$(mktemp)"; trap 'rm -f "$BODY"' EXIT

check() { # name expected actual
  if [ "$2" = "$3" ]; then echo "  PASS  $1"; PASS=$((PASS+1)); else echo "  FAIL  $1 (expected $2, got $3) body: $(head -c 300 "$BODY")"; FAIL=$((FAIL+1)); fi
}
code() { curl -s -o $BODY -w '%{http_code}' "$@"; }
body() { cat $BODY; }
json() { python3 -c "import sys,json; print(json.load(open(sys.argv[1]))$1)" "$BODY"; }

REG="{\"userName\":\"Smoke Tester\",\"email\":\"$EMAIL\",\"password\":\"S3cure!pass\"}"
LOGIN="{\"email\":\"$EMAIL\",\"password\":\"S3cure!pass\"}"
BAD_LOGIN="{\"email\":\"$EMAIL\",\"password\":\"wrong-pass\"}"
CT=(-H 'Content-Type: application/json')

echo "Gateway: $GW"
check "register (public)" 201 "$(code -X POST "$GW/auth/register" "${CT[@]}" -d "$REG")"
check "register duplicate -> 400" 400 "$(code -X POST "$GW/auth/register" "${CT[@]}" -d "$REG")"
check "login wrong password -> 401" 401 "$(code -X POST "$GW/auth/login" "${CT[@]}" -d "$BAD_LOGIN")"
check "login (public)" 200 "$(code -X POST "$GW/auth/login" "${CT[@]}" -d "$LOGIN")"
TOKEN=$(json "['token']")
AUTH=(-H "Authorization: Bearer $TOKEN")

check "no JWT -> 401" 401 "$(code "$GW/api/expenses")"
check "invalid JWT -> 401" 401 "$(code "$GW/api/expenses" -H 'Authorization: Bearer abc.def.ghi')"

check "create category" 201 "$(code -X POST "$GW/api/categories" "${AUTH[@]}" "${CT[@]}" -d '{"categoryName":"Groceries"}')"
CAT=$(json "['category_id']")
NEW_EXPENSE="{\"title\":\"Weekly groceries\",\"amount\":82.45,\"categoryId\":$CAT}"
check "create expense" 201 "$(code -X POST "$GW/api/expenses" "${AUTH[@]}" "${CT[@]}" -d "$NEW_EXPENSE")"
ID=$(json "['id']")
check "created expense has category name" "Groceries" "$(json "['categoryName']")"
LOCATION=$(curl -s -D - -o /dev/null -X POST "$GW/api/expenses" "${AUTH[@]}" "${CT[@]}" -d '{"title":"Bus pass","amount":15}' | tr -d '\r' | awk -F': ' 'tolower($1)=="location"{print $2}')
check "Location header points at gateway" "$GW/api/expenses" "${LOCATION%/*}"
check "fetch all" 200 "$(code "$GW/api/expenses" "${AUTH[@]}")"
check "fetch all count" 2 "$(json ".__len__()")"
check "fetch by id" 200 "$(code "$GW/api/expenses/$ID" "${AUTH[@]}")"
UPDATED="{\"title\":\"Monthly groceries\",\"amount\":310.00,\"categoryId\":$CAT}"
check "update" 200 "$(code -X PUT "$GW/api/expenses/$ID" "${AUTH[@]}" "${CT[@]}" -d "$UPDATED")"
check "updated title" "Monthly groceries" "$(json "['title']")"
check "invalid amount -> 400" 400 "$(code -X POST "$GW/api/expenses" "${AUTH[@]}" "${CT[@]}" -d '{"title":"Bad","amount":-5}')"
check "unknown id -> 404" 404 "$(code "$GW/api/expenses/999999" "${AUTH[@]}")"
check "category in use -> 409" 409 "$(code -X DELETE "$GW/api/categories/$CAT" "${AUTH[@]}")"
check "delete" 204 "$(code -X DELETE "$GW/api/expenses/$ID" "${AUTH[@]}")"
check "deleted -> 404" 404 "$(code "$GW/api/expenses/$ID" "${AUTH[@]}")"

# A second user must not see the first user's data
EMAIL2="smoke2.$(date +%s)@example.com"
REG2="{\"userName\":\"Other\",\"email\":\"$EMAIL2\",\"password\":\"S3cure!pass\"}"
LOGIN2="{\"email\":\"$EMAIL2\",\"password\":\"S3cure!pass\"}"
code -X POST "$GW/auth/register" "${CT[@]}" -d "$REG2" >/dev/null
code -X POST "$GW/auth/login" "${CT[@]}" -d "$LOGIN2" >/dev/null
TOKEN2=$(json "['token']")
check "other user sees 0 expenses" 0 "$(code "$GW/api/expenses" -H "Authorization: Bearer $TOKEN2" >/dev/null; json ".__len__()")"

echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
