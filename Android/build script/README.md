# Env.sh 

## DT 

### Process
```source Env.sh``` 진행 과정
1. ENV_Reset
2. set_common_information
3. if SELECT_BUILD else ENV_Help : 사용자 빌드타입 선택
4. case $PROJECT_TYPE : 
   선택된 빌드타입별로 함수 실행 대부분 user, userdebug, eng 환경설정에 testkey 확인 
   ENV_USER,USERDEBUG,ENG  , CHECKOUT_TESTKEY
5. DOWNLOAD_TTS_FILE
6. set_fw_information 

----

