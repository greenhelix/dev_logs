# BIST 
BIST Self Test
MVVM (Model View ViewModel) Architecture 





## 코드 설명 
- src 폴더 구성
View (TestFragment, MainActivity)
Model (Test, Test[aTest, bTest, cTest, ...])
ViewModel (MainViewModel, BaseTestViewModel, *TestViewModel)
Util (LogRepository, TestType, Status)

> - **ViewFragment - ViewModel -  Model 의 관계**
> 
> ViewModel은 UI의 생명주기(Life Cycle)와 분리되어 데이터를 보존하는 중요한 역할을 합니다.
> 
> ViewModelProvider는 이러한 ViewModel 인스턴스를 생성하고 관리해주는 핵심 클래스입니다.



