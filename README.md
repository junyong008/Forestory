## 🌲 숲토리

* 1인 프로젝트 [기획, 디자인, 프론트엔드, 백엔드]
* [플레이스토어](https://play.google.com/store/apps/details?id=com.yjy.forestory) 출시 



### 개요
* 인공지능 숲속 동물 친구들과 함께하는 나만의 SNS
* 일기장의 프라이빗함과 SNS의 소통의 장점을 합쳐보고자 기획

### 핵심 기능
* 게시글을 작성하면 AI가 게시글의 사진과 내용, 작성자 이름과 성별을 분석하여 각 AI 성격에 맞춰 댓글을 작성
* ListAdapter와 RoomDB Flow를 이용한 리사이클러뷰 데이터 실시간 최신화, Paging을 이용한 무한 스크롤 구현
* BaseActivity, BaseFragment 를 이용한 보일러플레이트 코드 최소화
* Hilt를 이용한 의존성 주입, 코드 재사용성 향상
* CoroutineWorker를 이용한 백그라운드 서버 연동, 데이터 백업/복원 처리
* MVVM 패턴 적용, ViewModel를 이용한 Configuration Change 대응
* LiveData, EventObserver, BindingAdapter 등을 이용한 데이터 무결성 보장
* 인앱 결제, 리워드 애드몹을 통한 재화 구매 및 사용

### 프로젝트 설계
![플레이스토어 그래픽 이미지](https://github.com/junyong008/Forestory/assets/69251013/62af564d-fa8e-40ea-8884-a5a7e2c5d3b2)
![플레이 스토어 앱 스크린샷 모음](https://github.com/junyong008/Forestory/assets/69251013/89c340e9-3744-436c-92f9-b3460a7cbee8)
![0](https://github.com/junyong008/Forestory/assets/69251013/5d3b5383-4db4-4934-be4c-cc1b3c930503)
![1](https://github.com/junyong008/Forestory/assets/69251013/0fda90d0-f357-48d5-8664-129d5bc47143)
![2](https://github.com/junyong008/Forestory/assets/69251013/3e398345-f0c2-4f2c-9325-835548c761ab)
![3 수정](https://github.com/junyong008/Forestory/assets/69251013/5e939ba8-f5aa-4d91-9442-a2115a854abe)
![4](https://github.com/junyong008/Forestory/assets/69251013/babb465b-c45f-4eac-b969-35e6a786bcdd)
