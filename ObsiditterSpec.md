## 概要

このアプリは、Jetpack Compose を使ったメモアプリです。
ローカルのmarkdownファイルにメモを保存します。
yyyy-MM-dd.md の形式で日次で保存してください。
下記のような、frontmatterと、 `## Journal` 配下にメモを時刻付きで追記してください。

```
---
date: "2025-12-29T12:39:00+09:00"
tags: 
    - 'daily'
fileClass: DailyLog
mood_morning:
wake_time:
mood_evening:
sleep_time:
snacks:
reading_min:
exercise_min:
---

## Memo

## Journal

- 12:39 
    今日の天気は晴れ
    30分歩いた
- 14:53 
    星を継ぐもの読みたくて有隣堂に買いに行ったが、レジが行列でやめた。
- 14:57
    自分用のメモアプリがほしいので作ろうかなあ。

```

### HomeScreen（メモ一覧画面）

- メモをtwitterのタイムライン風に一覧表示する
- 各メモの右に「編集（ペン）」と「削除（ゴミ箱）」アイコンを表示。
- 画面右下に「新規作成（＋）」FABを配置。

### AddNoteScreen（メモ新規作成）

- Home画面の「＋」ボタンで遷移。
- 入力項目：テキスト、ハッシュタグ
- 「登録」ボタンを押すと markdown に保存。

### EditNoteScreen（既存メモ編集）

- Home画面の「編集」ボタンで遷移。
- 入力項目は AddNoteScreen と同じだが、初期状態として既存データを表示。
- 「変更」ボタンで file を更新。

### 削除処理

- Home画面で「削除」ボタンを押すと、確認ダイアログを表示。
- 「はい」で該当メモを markdown から削除。

