# Obsiditter

Obsiditterは、Twitter/Xのようなマイクロブログのタイムライン風の見た目と使い心地を持つAndroidメモアプリですが、すべてのデータをローカルのMarkdownファイルとして保存します。これにより、[Obsidian](https://obsidian.md/)やその他のMarkdownベースのナレッジベースツールと完全な互換性があります。

## 特徴

- タイムラインインターフェース: おなじみの時系列順（新しい順）のタイムライン形式で日々のメモを閲覧できます。
- Markdown保存: すべてのメモは、ユーザーが選択したディレクトリ内のプレーンテキストMarkdownファイル（`YYYY-MM-DD.md`）に保存されます。
- Obsidian互換: ObsidianのDaily Notesプラグインとシームレスに連携するように設計されています。
  - メモは `## Journal` セクションに追記されます。
  - フォーマット: `- HH:mm 内容 #タグ`
- ローカル＆プライベート: クラウド同期は不要です。データは完全にあなたのものです。
- Storage Access Framework (SAF): デバイス上の任意のフォルダ（SDカードやSyncthingなどで同期されたフォルダを含む）内のファイルに安全にアクセスし、変更を加えることができます。
- Edge-to-Edgeデザイン: ジェスチャーナビゲーションとEdge-to-EdgeディスプレイをフルサポートしたモダンなUIです。

## 技術スタック

- 言語: Kotlin
- UIフレームワーク: Jetpack Compose (Material 3)
- アーキテクチャ: MVVM
- ファイルI/O: Android Storage Access Framework (SAF), `DocumentFile`
- Markdown解析: `org.jetbrains:markdown`

## 始め方

### 前提条件

- Android Studio Koala 以降
- JDK 17以上

### インストール

1.  リポジトリをクローンします:
    ```bash
    git clone https://github.com/ikorihn/obsidian-memos.git
    ```
2.  Android Studioでプロジェクトを開きます。
3.  Gradleプロジェクトを同期します。
4.  エミュレータまたは実機で実行します。

### 使い方

1.  初期設定: 初回起動時に「Select Folder」をタップして、Markdownファイルを保存するディレクトリを選択します。これは既存のObsidian保管庫（Vault）の「Daily Notes」フォルダを指定することも可能です。
2.  メモの作成:
    - ホーム画面下部の入力バーを使用します。
    - メモを入力し、必要に応じてハッシュタグを追加します。
    - 「Post」をタップすると、今日の日付のファイルにメモが追記されます。
3.  編集/削除:
    - メモの「編集」（鉛筆）アイコンをタップすると、その場で内容を修正できます。
    - メモの「削除」（ゴミ箱）アイコンをタップすると、メモを削除できます。

## ファイル形式

Obsiditterは `YYYY-MM-DD.md` という名前のファイルを読み書きします。
具体的には、`## Journal` という見出しを探して（なければ作成して）、その下のリスト項目を管理します。

例: `2024-01-01.md`

```markdown
---
date: "2024-01-01"
tags: [daily]
---

# 2024-01-01

## Journal

- 09:00 Started the day with coffee. #morning
- 12:30 Lunch at the new place.
- 18:00 Working on Obsiditter app. #coding
```
