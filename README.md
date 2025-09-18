## 🍃 가족과 함께 채우는 시니어 버킷리스트, 하나이음
> Digital Hana 路 금융서비스개발 6기 최종 프로젝트 우수상 🌟

하나이음은 시니어의 소중한 버킷리스트를 가족과 함께 이뤄가는 **금융 동행 플랫폼**입니다.
버킷리스트 목표를 세우고 필요한 자금을 머니 박스와 함께 모으며, 제휴 혜택과 응원으로 즐거움을 더합니다.

### 👋🏻 Contributors
> 디지털 하나로 6기 금융서비스개발 YOLDEN 팀

> `개발 기간` : 2025.08.13 ~ 2025.09.11

#### Backend Developers
|                                                                          김기보                                                                          |                                                                 손혜정                                                                 |                                                                   정재희                                                                    |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------------------------:|
| [<img src="https://github.com/user-attachments/assets/35657c0c-3d99-4e8c-8edc-2124798b2bbd" width=120> <br/> @KimGiii](https://github.com/KimGiii) |   [<img src="https://avatars.githubusercontent.com/u/74630428?v=4" width=120> <br/> @HyejeongSon](https://github.com/HyejeongSon)   |        [<img src="https://avatars.githubusercontent.com/u/127819805?v=4" width=120> <br/> @jaehejun](https://github.com/jaehejun)        |
|                                          버킷리스트 관련 기능<br>머니박스 관련 기능<br>버킷리스트 통합 테스트<br>버킷리스트 단위 테스트<br>문자 인증                                           |         Spring security, jwt 보안 시스템<br>주계좌/머니박스/거래 내역 처리<br>자동이체 시스템<br>응원 및 후원 기능<br>버킷리스트 삭제 및 달성<br>Github Action CI/CD          |                                  그룹 관련 기능<br>공유앨범 관련 기능<br>쿠폰 관련 기능<br>이자 계산 로직<br>AWS Cloud Watch 모니터링                                  |

### 📦 Package Structure

```  
com.hanaieum.server
├─ common
│  ├─ config              # 전역 설정 (SecurityConfig, SwaggerConfig, SolapiConfig)
│  ├─ dto                 # 공용 응답/에러 DTO
│  ├─ entity              # 기본 엔티티 (BaseEntity with auditing)
│  └─ exception           # 전역 예외 처리 (CustomException, ErrorCode, GlobalExceptionHandler)
│
├─ domain
│  ├─ account             # 계좌 관리 (Account, AccountType, AccountRepository, AccountService)
│  ├─ auth                # 인증 및 토큰 관리 (AuthController, RefreshToken, AuthService)
│  ├─ autoTransfer        # 자동이체 스케줄링 (AutoTransferSchedule, AutoTransferHistory, AutoTransferScheduler)
│  ├─ bucketList          # 버킷리스트 관리 (BucketList, BucketParticipant, BucketListService)
│  ├─ coupon              # 쿠폰 관리 (Coupon, MemberCoupon, CouponService)
│  ├─ group               # 그룹 관리 (Group, GroupRepository, GroupService)
│  ├─ member              # 회원 관리 (Member, MemberRepository, MemberService)
│  ├─ moneyBox            # 머니박스 관리 (MoneyBoxService)
│  ├─ photo               # 공유 앨범 관리 (Photo, PhotoRepository, PhotoService)
│  ├─ support             # 응원/후원 기능 (SupportRecord, SupportType, SupportService)
│  ├─ transaction         # 거래 내역 (Transaction, TransactionRepository, TransactionService)
│  ├─ transfer            # 이체 처리 (TransferService)
│  └─ verification        # 문자인증 (VerificationController, VerificationService)
│
├─ security
│  ├─ JwtTokenProvider           # 토큰 생성/검증
│  ├─ JwtAuthenticationFilter    # 인증 필터
│  └─ CustomUserDetails          # 사용자 인증 정보 관리
│
└─ infrastructure
   └─ aws                        # S3Config, S3Service
```
