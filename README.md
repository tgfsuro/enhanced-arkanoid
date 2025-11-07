# Game Project

## Mô tả
Dự án này là một trò chơi đơn giản sử dụng Java Swing, bao gồm các thành phần chính từ việc khởi động ứng dụng cho đến việc xử lý logic của trò chơi.

## Cấu trúc Dự án
game/
 ├─ GamePanel.java                 // điều phối: init, loop, input, paint gọi delegate
 ├─ LevelManager.java
 ├─ MusicHub.java
 ├─ play/
 │   ├─ ServingController.java     // logic bóng bám paddle + launch
 │   ├─ CollisionSystem.java       // va chạm bóng–tường–paddle–gạch
 │   ├─ ProjectileManager.java     // đạn & laser (enableGun, update, render)
 │   ├─ DropManager.java           // power-up rơi, applyDrop(...)
 │   └─ ScoreKeeper.java           // cộng điểm & HUD data (tuỳ thích)
 └─ ui/
     ├─ HudOverlay.java            // vẽ score/lives/level + nút
     └─ SettingsOverlay.java       // vẽ & xử lý overlay cài đặt
     
