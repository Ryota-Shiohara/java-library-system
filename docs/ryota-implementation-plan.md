# ryota 実装計画書

## 1. 担当の目的

ryotaは「本を正しく管理し、データを安全に保存できる基盤」を担当する。モデル、業務ロジック、永続化、GUI、JUnitテストを一通り受け持ち、単なる共通作業担当にならないように成果物を明確にする。

想定作業時間は約25時間とする。

## 2. 主担当範囲

### 本モデルと在庫管理

- `Book`モデルを実装する。
- 書籍ID、タイトル、ジャンル、所有冊数を保持する。
- 貸出中冊数はLoan一覧から算出し、二重管理を避ける。
- 利用可能冊数を`totalCopies - activeLoanCount`で算出する。
- 空文字、重複ID、不正な冊数を拒否する。
- 貸出中冊数を下回る所有冊数への変更を拒否する。
- 貸出中の本の削除を拒否する。

### 本の業務ロジック

- 本の追加、修正、削除、一覧、ID検索を実装する。
- タイトルとジャンルの部分一致検索を実装する。
- 並べ替えに利用できる読み取り専用の一覧を返す。
- GUIに依存しないService APIとして実装する。

### 永続化基盤

- 保存ディレクトリとファイル形式を定義する。
- UTF-8で保存・読込を行う。
- タブ、改行、区切り文字を含む入力でも壊れないエスケープ方式を決める。
- 一時ファイルへ書き込んでから置換し、途中終了による破損を減らす。
- 初回起動でデータファイルがない場合は空データとして扱う。
- kumpeiが会員・貸出データを保存できる共通I/Oを提供する。

### GUI

- `Main`とメインウィンドウの枠を実装する。
- Booksタブの一覧テーブルを実装する。
- Add、Edit、Delete、Search、Clear操作を実装する。
- Total、Loaned、Availableの各冊数を表示する。
- 入力エラーを英語のダイアログで表示する。
- kumpeiのMembersタブとLoansタブを組み込めるAPIを用意する。

## 3. 想定成果物

```text
src/main/java/library/model/Book.java
src/main/java/library/repository/DataStore.java
src/main/java/library/repository/FileDataStore.java
src/main/java/library/service/BookService.java
src/main/java/library/ui/MainFrame.java
src/main/java/library/ui/book/BookPanel.java
src/main/java/library/ui/book/BookDialog.java
src/main/java/library/Main.java

src/test/java/library/model/BookTest.java
src/test/java/library/repository/FileDataStoreTest.java
src/test/java/library/service/BookServiceTest.java
```

実際のクラス名は初日の共同設計で確定し、確定後は勝手に公開APIを変更しない。

### 実装言語に関する規則

ryotaが担当するすべての実装は英語で記述する。次の項目には日本語を一切含めない。

- クラス名、メソッド名、変数名、定数名、パッケージ名、ファイル名。
- 行コメント、ブロックコメント、Javadoc、TODO、FIXME。
- JUnitのテストクラス名、テストメソッド名、`@DisplayName`、テスト用説明文。
- Books画面のボタン、ラベル、テーブル見出し、ダイアログ。
- 例外メッセージ、入力エラー、ログ、コンソール出力。
- ソースコード内のサンプルデータ。

日本語を使用できるのはREADME、`docs/`内の文書、提出レポートに限る。PRをReady for reviewへ変更する前に、担当ソースへ日本語が混入していないことを確認する。

## 4. JUnitテスト

ryotaは次のテストを主担当とする。

### Book

- 正しい値で本を生成できる。
- タイトル、ジャンル、IDが空の場合に失敗する。
- 所有冊数が不正な場合に失敗する。
- 利用可能冊数が正しく計算される。

### BookService

- 本を追加・修正・削除・検索できる。
- 重複IDを拒否する。
- 貸出中の本を削除できない。
- 所有冊数を貸出中冊数より小さく変更できない。
- 検索で大文字小文字の違いを適切に扱う。
- 失敗した操作の後に状態が変化していない。

### FileDataStore

- 複数件を保存して同じ内容を読み戻せる。
- データファイルがない場合に空一覧を返す。
- 空白や記号を含む文字列を往復できる。
- 壊れたレコードを検出し、分かりやすい例外に変換する。
- JUnitの`@TempDir`を使い、実データを汚さない。

テストでは実装の内部構造ではなく、公開APIから観察できる振る舞いを検証する。

## 5. 日別計画

### 7月13日

- kumpeiとモデル、Service API、削除制約、保存形式を決める。
- Books画面のワイヤーフレームを共有する。
- `feature/ryota-books`ブランチを作る。

### 7月14日

- `Book`と本管理Serviceの骨格を実装する。
- 対応するJUnitテストを先に作成する。
- 共通永続化インターフェースを実装する。
- Book、BookService、Repositoryの公開契約を含むDraft PRを出す。

### 7月15日

- 本のCRUD、検索、在庫制約を完成させる。
- Books画面と編集ダイアログを完成させる。
- Draft PRへ実装とテストを追加し、Ready for reviewへ変更してkumpeiへレビューを依頼する。

### 7月16日

- kumpeiのLoan APIと在庫計算を接続する。
- 貸出・返却後にBooks画面が更新されることを確認する。

### 7月17日

- 本・会員・貸出の保存処理を統合する。
- 再起動後の復元テストを追加する。
- 永続化統合を独立したPRとして出す。

### 7月18日

- kumpei担当コードをレビューする。
- 境界値、例外処理、検索、保存失敗時の動作を改善する。
- 検索、境界値テスト、レビュー修正を小さなPRとして出す。

### 7月19日以降

- レポートの設計・保存方式・テスト方針を執筆する。
- kumpeiとクリーン環境でコンパイル・実行確認を行う。
- 7月19日に提出候補版の最終PRを二人でレビューし、mainへマージする。
- 7月20日は不具合修正と提出確認だけに使う。

## 6. Pull Request計画

| 期限 | PR | Ready for reviewの条件 | 予定マージ |
|---|---|---|---|
| 7月14日 | Draft: Book model and catalog contracts | 公開API、データ項目、不変条件、テスト骨格が確認できる | kumpeiと契約を合意し、共通基盤だけを7月14日夜にマージ |
| 7月15日 | Books CRUD and Swing UI | 本のCRUDと検索が動き、担当JUnitが全件成功する | kumpeiのApprove後、7月15日夜にマージ |
| 7月17日 | File persistence integration | 保存・再読込と初回起動テストが成功する | 全モデルの復元を二人で確認後、7月17日夜にマージ |
| 7月18日 | Catalog edge cases and review fixes | 境界値テスト成功、kumpeiの指摘対応、実装内に日本語がない | 全テスト成功後、7月18日夜にマージ |

公開APIを変更する場合は、変更コードを書く前にDraft PR上でkumpeiの合意を得る。PR本文には担当範囲、確認手順、JUnit結果、未完了項目を記載する。1つの巨大なPRにまとめず、上表の単位でレビュー可能な差分を保つ。マージ後はkumpeiへ連絡し、kumpeiの作業ブランチへ最新の`main`を取り込んでもらう。

## 7. kumpeiへ渡す契約

- 本の取得は変更不能な値またはコピーで返す。
- 貸出可否を判断するための`availableCopies(bookId)`を提供する。
- 貸出中冊数の計算に必要なLoan参照方法を共同で決める。
- 保存処理は本・会員・貸出を一貫したスナップショットとして扱う。
- UI更新通知の方法を決め、kumpei側が本の内部状態を直接変更しないようにする。

## 8. レビュー観点

kumpeiのコードについて、次を確認する。

- 在庫0で貸出が成立しないこと。
- 会員・貸出の削除制約が本側の制約と矛盾しないこと。
- GUIからServiceを迂回してデータを変更していないこと。
- 返却後に在庫と画面表示が同期すること。
- 例外メッセージ、画面表示、コメントに日本語がないこと。
- Javadoc、テスト名、TODO、ログ、サンプルデータにも日本語がないこと。
- テストが順番や既存ファイルに依存しないこと。

## 9. 完了条件

- 本の必須機能をGUIとServiceの両方から確認できる。
- ryota担当のJUnitテストがすべて成功する。
- kumpeiの貸出返却処理と統合して在庫数が一致する。
- 保存・再読込で本の情報が失われない。
- コメント、Javadoc、テスト名、UI、例外・ログを含む実装全体が英語である。
- ryota担当Pull Requestをkumpeiがレビュー済みである。
- レポートに実装内容、判断理由、テスト結果を具体的に記載できる。
