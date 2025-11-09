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
