# Deferred Items

## 2026-03-22

- Backend targeted verification for `05-00` is partially blocked outside this plan's touched files.
  - `mvn "-Dtest=NotificationControllerTest,NotificationServiceTest,NotificationPushServiceTest" test` only runs after forcing `JAVA_HOME` to `C:\Program Files\Java\latest\jdk-21`, but `NotificationControllerTest` then fails to boot the application context because `ChatController` depends on a missing `ChatMapper` bean.
  - `mvn clean "-Dtest=NotificationControllerTest,NotificationServiceTest,NotificationPushServiceTest" test` exposes unrelated pre-existing `testCompile` failures in `backend/src/test/java/com/tradingplatform/listing/service/ListingSearchServiceTest.java` and `backend/src/test/java/com/tradingplatform/listing/service/ListingServiceTest.java`.
  - These issues were not introduced by the Wave 0 scaffold edits and should be resolved before relying on the backend Maven verification command for later Phase 5 plans.
