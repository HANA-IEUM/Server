import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 100,         // 100명의 가상 사용자 (스트레스 테스트)
    duration: '2m',    // 2분 동안 테스트 실행
    thresholds: {
        http_req_duration: ['p(95)<1000'], // 100명일 때는 기준을 1초로 완화
        http_req_failed: ['rate<0.10'],   // 에러율 10% 미만 목표
    },
};

const BASE_URL = 'http://localhost:8080/api';

// 무작위 데이터 생성을 위한 헬퍼 함수
function getRandomPhoneNumber() {
    // 010 + 무작위 8자리 숫자
    const randomDigits = Math.floor(10000000 + Math.random() * 90000000);
    return `010${randomDigits}`;
}

export default function () {
    const phoneNumber = getRandomPhoneNumber();
    const password = '123456'; 
    const name = `테스터_${__VU}_${__ITER}`;

    const headers = { 'Content-Type': 'application/json' };

    // --- 1단계: 회원가입 ---
    const signupPayload = JSON.stringify({
        phoneNumber: phoneNumber,
        password: password,
        name: name,
        birthDate: '1995-01-01',
        gender: 'M',
        monthlyLivingCost: 500000
    });

    const signupRes = http.post(`${BASE_URL}/auth/register`, signupPayload, { headers });
    
    if (signupRes.status !== 201) {
        console.error(`회원가입 실패 [${signupRes.status}]: ${signupRes.body}`);
    }
    check(signupRes, { '회원가입 성공 (201)': (r) => r.status === 201 });

    if (signupRes.status !== 201) {
        sleep(1);
        return;
    }

    // --- 2단계: 로그인 (토큰 획득) ---
    const loginPayload = JSON.stringify({
        phoneNumber: phoneNumber,
        password: password
    });

    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, { headers });
    const loginData = loginRes.json();
    const token = loginData.data ? loginData.data.accessToken : null;
    
    check(loginRes, { '로그인 성공 (200)': (r) => r.status === 200 });

    if (!token) {
        console.error(`로그인 실패 [${loginRes.status}]: ${loginRes.body}`);
        return;
    }

    const authHeaders = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    // --- 3단계: 버킷리스트 생성 ---
    const createPayload = JSON.stringify({
        type: 'TRIP', 
        title: `부하 테스트 버킷리스트_${name}`,
        targetAmount: 1000000,
        targetMonths: '12',
        publicFlag: true,
        togetherFlag: false,
        createMoneyBox: true
    });

    const createRes = http.post(`${BASE_URL}/bucket-lists`, createPayload, authHeaders);
    
    if (createRes.status !== 201) {
        console.error(`버킷리스트 생성 실패 [${createRes.status}]: ${createRes.body}`);
    }

    check(createRes, { '버킷리스트 생성 성공 (201)': (r) => r.status === 201 });

    const createData = createRes.json();
    const bucketListId = createData.data ? createData.data.id : null;

    // --- 4단계: 버킷리스트 수정 및 삭제 ---
    if (bucketListId) {
        // 수정
        const updatePayload = JSON.stringify({
            title: `수정된_${name}`,
            publicFlag: false,
            shareFlag: false
        });
        const updateRes = http.patch(`${BASE_URL}/bucket-lists/${bucketListId}`, updatePayload, authHeaders);
        check(updateRes, { '수정 성공 (200)': (r) => r.status === 200 });

        // 삭제
        const deleteRes = http.del(`${BASE_URL}/bucket-lists/${bucketListId}`, null, authHeaders);
        check(deleteRes, { '삭제 성공 (200)': (r) => r.status === 200 });
    }

    sleep(1); 
}
