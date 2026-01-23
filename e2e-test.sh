#!/bin/bash

# E2E 주문 테스트 스크립트
# 카테고리 생성 -> Owner 회원가입/로그인 -> 상품 등록 -> User 회원가입/로그인 -> 배송지 등록 -> 주문 생성 -> Payment READY 확인

set -e

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_step() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

log_success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
}

log_error() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}

log_info() {
    echo -e "${YELLOW}[INFO] $1${NC}"
}

# JSON 파싱 (jq 사용)
json_get() {
    echo "$1" | jq -r "$2"
}

# ===========================================
# 0. 서버 상태 확인
# ===========================================
log_step "0. 서버 상태 확인"

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/categories" 2>/dev/null || echo "000")
if [ "$HTTP_STATUS" != "200" ]; then
    log_error "서버가 실행되지 않았습니다. (HTTP: $HTTP_STATUS)"
fi
log_success "서버 정상 동작 중"

# ===========================================
# 1. 카테고리 DB 직접 생성
# ===========================================
log_step "1. 카테고리 DB 직접 생성"

CATEGORY_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
CATEGORY_NAME="테스트카테고리_${TIMESTAMP}"

docker exec postgres-ecommerce psql -U postgres -d ecommerce -c "
INSERT INTO p_category (category_id, name, depth, sort_order, is_active, created_at, updated_at)
VALUES ('${CATEGORY_ID}', '${CATEGORY_NAME}', 0, 1, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
" > /dev/null 2>&1

log_success "카테고리 생성 완료: ${CATEGORY_NAME}"
log_info "Category ID: ${CATEGORY_ID}"

# ===========================================
# 2. Owner 회원가입
# ===========================================
log_step "2. Owner 회원가입"
# (이하 동일 — 변경 없음)


OWNER_EMAIL="owner_${TIMESTAMP}@test.com"
OWNER_PASSWORD="Test1234!"

OWNER_SIGNUP_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"${OWNER_EMAIL}\",
        \"password\": \"${OWNER_PASSWORD}\",
        \"nickname\": \"TestOwner_${TIMESTAMP}\",
        \"phoneNumber\": \"010-1111-1111\",
        \"role\": \"OWNER\",
        \"store\": \"테스트스토어\",
        \"zipCode\": \"12345\",
        \"address\": \"서울시 강남구\",
        \"detailAddress\": \"테스트빌딩 1층\",
        \"bank\": \"신한은행\",
        \"account\": \"110-123-456789\"
    }")

HTTP_CODE=$(echo "$OWNER_SIGNUP_RES" | tail -1)
if [ "$HTTP_CODE" != "201" ]; then
    log_error "Owner 회원가입 실패 (HTTP: $HTTP_CODE)"
fi
log_success "Owner 회원가입 완료: ${OWNER_EMAIL}"

# ===========================================
# 3. Owner 로그인
# ===========================================
log_step "3. Owner 로그인"

OWNER_LOGIN_RES=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"${OWNER_EMAIL}\", \"password\": \"${OWNER_PASSWORD}\"}")

OWNER_TOKEN=$(json_get "$OWNER_LOGIN_RES" '.accessToken')
if [ -z "$OWNER_TOKEN" ] || [ "$OWNER_TOKEN" == "null" ]; then
    log_error "Owner 로그인 실패"
fi
log_success "Owner 로그인 완료"
log_info "Token: ${OWNER_TOKEN:0:50}..."

# ===========================================
# 4. 상품 등록
# ===========================================
log_step "4. 상품 등록"

PRODUCT_TITLE="테스트상품_${TIMESTAMP}"
PRODUCT_PRICE=15000
STOCK_QUANTITY=100

PRODUCT_CREATE_RES=$(curl -s -X POST "$BASE_URL/api/v1/owner/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -d "{
        \"categoryId\": \"${CATEGORY_ID}\",
        \"title\": \"${PRODUCT_TITLE}\",
        \"description\": \"E2E 테스트용 상품입니다.\",
        \"thumbnailUrl\": \"https://example.com/test.jpg\",
        \"hasOptions\": false,
        \"price\": ${PRODUCT_PRICE},
        \"stockQuantity\": ${STOCK_QUANTITY}
    }")

PRODUCT_ID=$(json_get "$PRODUCT_CREATE_RES" '.productId')
if [ -z "$PRODUCT_ID" ] || [ "$PRODUCT_ID" == "null" ]; then
    echo "Response: $PRODUCT_CREATE_RES"
    log_error "상품 등록 실패"
fi
log_success "상품 등록 완료: ${PRODUCT_TITLE}"
log_info "Product ID: ${PRODUCT_ID}"

# ===========================================
# 5. User 회원가입
# ===========================================
log_step "5. User 회원가입"

USER_EMAIL="user_${TIMESTAMP}@test.com"
USER_PASSWORD="Test1234!"

USER_SIGNUP_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"${USER_EMAIL}\",
        \"password\": \"${USER_PASSWORD}\",
        \"nickname\": \"TestUser_${TIMESTAMP}\",
        \"phoneNumber\": \"010-2222-2222\",
        \"role\": \"USER\"
    }")

HTTP_CODE=$(echo "$USER_SIGNUP_RES" | tail -1)
if [ "$HTTP_CODE" != "201" ]; then
    log_error "User 회원가입 실패 (HTTP: $HTTP_CODE)"
fi
log_success "User 회원가입 완료: ${USER_EMAIL}"

# ===========================================
# 6. User 로그인
# ===========================================
log_step "6. User 로그인"

USER_LOGIN_RES=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"${USER_EMAIL}\", \"password\": \"${USER_PASSWORD}\"}")

USER_TOKEN=$(json_get "$USER_LOGIN_RES" '.accessToken')
USER_ID=$(json_get "$USER_LOGIN_RES" '.userId')
if [ -z "$USER_TOKEN" ] || [ "$USER_TOKEN" == "null" ]; then
    log_error "User 로그인 실패"
fi
log_success "User 로그인 완료"
log_info "User ID: ${USER_ID}"

# ===========================================
# 7. 배송지 등록
# ===========================================
log_step "7. 배송지 등록"

ADDRESS_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/users/me/addresses" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    -d '{
        "zipCode": "06234",
        "address": "서울특별시 강남구 테헤란로 123",
        "detailAddress": "테스트아파트 101동 1001호",
        "recipient": "홍길동",
        "recipientPhone": "010-3333-3333",
        "isDefault": true
    }')

HTTP_CODE=$(echo "$ADDRESS_RES" | tail -1)
if [ "$HTTP_CODE" != "200" ]; then
    echo "Response: $ADDRESS_RES"
    log_error "배송지 등록 실패 (HTTP: $HTTP_CODE)"
fi
log_success "배송지 등록 완료"

# ===========================================
# 8. 상품 조회 (가격 확인)
# ===========================================
log_step "8. 상품 정보 조회"

PRODUCT_DETAIL=$(curl -s "$BASE_URL/api/v1/products/${PRODUCT_ID}")
ACTUAL_PRICE=$(json_get "$PRODUCT_DETAIL" '.price')
log_success "상품 조회 완료"
log_info "상품명: $(json_get "$PRODUCT_DETAIL" '.title')"
log_info "가격: ${ACTUAL_PRICE}원"

# ===========================================
# 9. 주문 생성
# ===========================================
log_step "9. 주문 생성"

QUANTITY=1
TOTAL_AMOUNT=$((ACTUAL_PRICE * QUANTITY))

ORDER_RES=$(curl -s -X POST "$BASE_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    -d "{
        \"totalAmount\": ${TOTAL_AMOUNT},
        \"items\": [{
            \"productId\": \"${PRODUCT_ID}\",
            \"variantId\": null,
            \"productTitle\": \"${PRODUCT_TITLE}\",
            \"productThumbnail\": \"https://example.com/test.jpg\",
            \"optionName\": null,
            \"unitPrice\": ${ACTUAL_PRICE},
            \"quantity\": ${QUANTITY}
        }]
    }")

ORDER_ID=$(json_get "$ORDER_RES" '.orderId')
if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" == "null" ]; then
    echo "Response: $ORDER_RES"
    log_error "주문 생성 실패"
fi
log_success "주문 생성 완료"
log_info "Order ID: ${ORDER_ID}"

# ===========================================
# 10. 주문 상태 확인
# ===========================================
log_step "10. 주문 상태 확인"

sleep 1

ORDER_DETAIL=$(curl -s "$BASE_URL/api/v1/orders/${ORDER_ID}" \
    -H "Authorization: Bearer ${USER_TOKEN}")

ORDER_STATUS=$(json_get "$ORDER_DETAIL" '.status')
ORDER_NO=$(json_get "$ORDER_DETAIL" '.orderNo')
log_success "주문 상태: ${ORDER_STATUS}"
log_info "주문번호: ${ORDER_NO}"

# ===========================================
# 11. Payment READY 상태 확인
# ===========================================
log_step "11. Payment READY 상태 확인"

sleep 2

PAYMENT_STATUS=$(docker exec postgres-ecommerce psql -U postgres -d ecommerce -t -c "
SELECT status FROM p_payment WHERE order_id = '${ORDER_ID}';
" | tr -d ' \n')

if [ "$PAYMENT_STATUS" == "READY" ]; then
    log_success "Payment 상태: ${PAYMENT_STATUS}"
else
    log_error "Payment 상태가 READY가 아닙니다. 현재 상태: ${PAYMENT_STATUS}"
fi


# ===========================================
# 결과 요약
# ===========================================
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}         E2E 테스트 완료!              ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Category ID : ${CATEGORY_ID}"
echo -e "Product ID  : ${PRODUCT_ID}"
echo -e "Order ID    : ${ORDER_ID}"
echo -e "Order No    : ${ORDER_NO}"
echo -e "Order Status: ${ORDER_STATUS}"
echo -e "Payment     : ${PAYMENT_STATUS}"
echo ""
