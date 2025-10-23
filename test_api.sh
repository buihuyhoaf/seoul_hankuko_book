#!/bin/bash

# Script để test API và kiểm tra data
echo "=== Testing Korean Learning API ==="

# Base URL
BASE_URL="http://192.168.1.23:8000/api/v1"

# Test lesson API
echo "1. Testing lesson API..."
echo "GET $BASE_URL/lessons/1"

# Test với curl (nếu có)
if command -v curl &> /dev/null; then
    echo "Response:"
    curl -s "$BASE_URL/lessons/1" | jq '.' 2>/dev/null || curl -s "$BASE_URL/lessons/1"
else
    echo "curl not available, please test manually"
fi

echo ""
echo "2. Testing quiz API..."
echo "GET $BASE_URL/quizzes/1"

if command -v curl &> /dev/null; then
    echo "Response:"
    curl -s "$BASE_URL/quizzes/1" | jq '.' 2>/dev/null || curl -s "$BASE_URL/quizzes/1"
else
    echo "curl not available, please test manually"
fi

echo ""
echo "3. Testing course API..."
echo "GET $BASE_URL/courses"

if command -v curl &> /dev/null; then
    echo "Response:"
    curl -s "$BASE_URL/courses" | jq '.' 2>/dev/null || curl -s "$BASE_URL/courses"
else
    echo "curl not available, please test manually"
fi

echo ""
echo "=== Manual Testing Instructions ==="
echo "1. Open browser and go to: $BASE_URL/lessons/1"
echo "2. Check if lesson has quizzes"
echo "3. Check if quizzes have questions"
echo "4. Check if questions have options"
echo ""
echo "=== Database Check ==="
echo "Run these SQL queries in pgAdmin:"
echo "SELECT * FROM lessons LIMIT 5;"
echo "SELECT * FROM quizzes LIMIT 5;"
echo "SELECT * FROM questions LIMIT 5;"
echo "SELECT * FROM question_options LIMIT 5;"

