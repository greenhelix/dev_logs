

1. PowerKeyLock ON 은 STB -> STB/TV/AVR 로 전원 컨트롤을 할 수 있게 한다.
2. PowerKeyLock ON 에서 RCU setup 이 안되있으면 RCU setup 을 먼저 실행하여 설정한다. 
3. PowerKeyLock ON 에서 RCU setup 이 완료되어있으면 STB -> STB/TV/AVR 로 전원 컨트롤을 한다. 
4. PowerKeyLock ON 에서 RCU setup 을 중간에 종료하면 STB 로 유지하고 PowerKeyLock OFF 로 유지한다. 
5. PowerKeyLock ON 에서 OFF 로 바꾸면 STB/TV/AVR -> STB 로 전원 컨트롤 한다. 
6. PowerKeyLock ON 하면 ExpertSetting 에 Switch on/off together 가 OFF 된다. 
   1. #INNOPIA: OFF가 되었을때 switch on/off together 가 DISABLE(선택안됨) 상태가 되야하는지 DT에 확인이 필요하다. G7보드에서는 그렇게 구현되어 있다. 
7. PowerKeyLock OFF 하면 ExpertSetting 에 Switch on/off together 가 ON 된다.
   1. #INNOPIA: 6번과 같이 DISABLE(선택안됨) 상태가 맞는지 아니면 선택이 되게 한다면, Switch on/off together가 off를 시키면 DISABLE 상태가 되야 하는지 명확하지 않다.

1. Reset RCU IR Config 를 Click 하면 RCU의 UDB 를 clear 시킨다. 
2. .  Reset RCU IR Config 를 Click 하면 RCU의 UDB 를 clear 하면, PowerKeyLock 이 ON 이라면 OFF 로 변경된다. 
3.  



### RCU 가 없으면, RCU Setup App이 진행되지 않는다. 즉, continue를 눌러도 다음 셋팅 과정으로 안 넘어가고 멈춰있다.



