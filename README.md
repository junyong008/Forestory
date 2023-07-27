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
* Hilt를 이용한 의존성 주입, 메인으로 어플이 실행되는 동안 유지되는 뷰모델은 싱글톤으로 구성
* CoroutineWorker를 이용한 백그라운드 서버 연동, 데이터 백업/복원 처리
* MVVM 패턴 적용, ViewModel를 이용한 Configuration Change 대응

### 프로젝트 설계
![플레이스토어 그래픽 이미지](https://github.com/junyong008/Forestory/assets/69251013/39ea1be1-a04a-4f8a-a90c-5400978e95d7)
![플레이 스토어 앱 스크린샷 모음](https://github.com/junyong008/Forestory/assets/69251013/4b688f86-f856-45f0-90d9-b4122b306e07)
![0](https://github.com/junyong008/Forestory/assets/69251013/13911e59-3b67-48b0-a2c9-6e115cef9a64)
![1](https://github.com/junyong008/Forestory/assets/69251013/95356f0a-8fbe-49dc-9131-cbd707ea322a)
![2](https://github.com/junyong008/Forestory/assets/69251013/b19ad730-624e-4f8d-8ce4-972a6ef93fc6)
![3](https://github.com/junyong008/Forestory/assets/69251013/83e0acfd-a4f9-4bc9-b10a-a9e4a704c69d)
![4](https://github.com/junyong008/Forestory/assets/69251013/bb8aae24-669a-43d9-89ab-d96e5751f171)
