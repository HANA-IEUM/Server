#!/bin/bash

BASE_URL="http://localhost:8080/api"
PHONE="01011112222"
PW="123456"

echo "1. 회원가입 시도..."
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"phoneNumber\": \"$PHONE\",
    \"password\": \"$PW\",
    \"name\": \"TestUser\",
    \"birthDate\": \"1995-05-05\",
    \"gender\": \"M\",
    \"monthlyLivingCost\": 500000
  }"
echo -e "\n"

echo "2. 로그인 시도..."
LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"phoneNumber\": \"$PHONE\",
    \"password\": \"$PW\"
  }")
TOKEN=$(echo $LOGIN_RES | grep -oE '"accessToken":"[^"]+"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "로그인 실패! 토큰을 받지 못했습니다."
  exit 1
fi
echo "로그인 성공. 토큰 획득 완료."

echo "3. 자동이체 포함 버킷리스트 생성 시도 (오늘 날짜 26일)..."
CREATE_RES=$(curl -s -X POST "$BASE_URL/bucket-lists" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"type\": \"TRIP\",
    \"title\": \"AutoTransfer Test\",
    \"targetAmount\": 5000000,
    \"targetMonths\": \"12\",
    \"publicFlag\": true,
    \"togetherFlag\": false,
    \"createMoneyBox\": true,
    \"moneyBoxName\": \"AutoTransfer MoneyBox\",
    \"enableAutoTransfer\": true,
    \"monthlyAmount\": 100000,
    \"transferDay\": \"26\"
  }")
echo "생성 응답: $CREATE_RES"
BL_ID=$(echo $CREATE_RES | grep -oE '"id":[0-9]+' | head -1 | cut -d':' -f2)
echo "추출된 BL_ID: $BL_ID"

echo "잠시 기다리는 중... (스케줄러가 실행될 때까지 1분 대기)"
sleep 65

echo -e "\n--- 최종 메트릭 확인 (자동이체 포함) ---"
curl -s http://localhost:8080/actuator/prometheus | grep hana_ieum
