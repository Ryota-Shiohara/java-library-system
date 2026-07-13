# 図書館システム共有仕様

## 1. この文書の扱い

この文書を、ryotaとkumpeiが実装する図書館システムの共有仕様とする。実装、テスト、GUI、保存データがこの文書と食い違う場合は、この文書を基準に修正する。

共有仕様を変更するときは、変更コードを書く前に二人で合意し、この文書と関連する公開APIを同じPull Requestで更新する。

## 2. 実装全体の原則

- 本体はJava SEだけで動作させ、Java 17を対象にコンパイルする。
- GUIはJava Swingで実装する。
- GUIからモデルやファイルを直接変更せず、`UI -> Service -> Repository -> DataStore`の順に呼び出す。
- モデルは変更不能な値として扱い、更新時は新しい値を作成する。
- RepositoryとServiceが返す一覧は、呼び出し側から変更できないようにする。
- 実装コード、コメント、Javadoc、テスト、UI、例外メッセージ、ログには日本語を含めない。
- README、`docs/`内の文書、提出レポートでは日本語を使用できる。
- 利用者が入力するタイトルや氏名には、英語以外を含む任意のUnicode文字を保存できる。

## 3. パッケージ構成

```text
src/main/java/library/
├─ Main.java
├─ exception/
├─ model/
├─ repository/
├─ service/
│  └─ dto/
└─ ui/
   ├─ book/
   ├─ member/
   └─ loan/

src/test/java/library/
├─ model/
├─ repository/
└─ service/
```

パッケージ間の依存方向は次のとおりとする。

```text
ui -> service -> repository -> model
             \-> model
```

`model`は他のアプリケーションパッケージへ依存しない。`repository`はSwingへ依存しない。`service`はSwingや具体的なファイル形式へ依存しない。

## 4. IDと文字列の共通規則

### 4.1 本IDと会員ID

- 型は`String`とする。
- 利用者が入力する。
- `String.strip()`で前後の空白を除去し、`Locale.ROOT`を使って大文字へ正規化してから保存・比較する。
- 使用できる文字は半角英数字、ハイフン、アンダースコアとする。
- 長さは1文字以上32文字以下とする。
- 正規表現は`[A-Z0-9][A-Z0-9_-]{0,31}`とする。
- 大文字・小文字だけが異なるIDは同じIDとして扱う。
- ServiceとRepositoryのIDを受け取る全メソッドで、検索・比較前に同じ正規化を行う。
- 既存IDと重複する追加は`DuplicateIdException`で拒否する。
- 登録後にIDは変更できない。編集画面ではIDを読み取り専用にする。

### 4.2 貸出ID

- 型は`String`とする。
- `LoanService`が`UUID.randomUUID().toString()`で生成する。
- 利用者には入力させない。
- テストではID生成処理を`Supplier<String>`として注入できるようにする。

### 4.3 表示文字列

- タイトル、ジャンル、氏名は`String.strip()`で前後の空白を除去して保存する。
- `String.isBlank()`が`true`になる値は`ValidationException`で拒否する。
- 内部に空白、タブ、改行、区切り文字、Unicode文字を含む値は許可する。
- 検索は`Locale.ROOT`を用いた大文字・小文字を区別しない部分一致とする。
- 空の検索語は全件を返す。

## 5. ドメインモデル

モデルはJavaの`record`で実装する。コンストラクタで単項目の妥当性を検証し、複数モデルにまたがる制約はServiceで検証する。

### 5.1 Book

```java
public record Book(
        String id,
        String title,
        String genre,
        int totalCopies) {
}
```

- `id`、`title`、`genre`は空にできない。
- `totalCopies`は1以上とする。
- 貸出中冊数は`Book`に保存せず、アクティブな`Loan`から算出する。
- 利用可能冊数は`totalCopies - loanedCopies`とする。
- 同じタイトルを持つ別の本IDは登録できる。

### 5.2 Member

```java
public record Member(
        String id,
        String name) {
}
```

- `id`と`name`は空にできない。
- 借りている本は`Member`に保存せず、アクティブな`Loan`から取得する。

### 5.3 Loan

```java
public record Loan(
        String id,
        String bookId,
        String memberId,
        LocalDate checkoutDate,
        LocalDate dueDate) {
}
```

- `id`、`bookId`、`memberId`、`checkoutDate`、`dueDate`は`null`にできない。
- `dueDate`は`checkoutDate.plusDays(14)`とする。
- 保存するのは現在貸出中の`Loan`だけとする。
- 返却時は対象の`Loan`を削除する。
- 貸出履歴は今回の必須実装に含めない。

## 6. 業務ルール

### 6.1 本の管理

- 本を追加、修正、削除、一覧表示、ID検索できる。
- タイトルとジャンルを対象に検索できる。
- 本の修正ではタイトル、ジャンル、所有冊数だけを変更できる。
- 所有冊数を現在の貸出中冊数より小さく変更できない。
- アクティブな貸出が1件でもある本は削除できない。

### 6.2 会員の管理

- 会員を追加、修正、削除、一覧表示、ID検索できる。
- 会員IDと氏名を対象に検索できる。
- 会員の修正では氏名だけを変更できる。
- アクティブな貸出が1件でもある会員は削除できない。

### 6.3 貸出

貸出は次の条件をすべて満たす場合だけ成立する。

1. 本IDに対応する本が存在する。
2. 会員IDに対応する会員が存在する。
3. 本の利用可能冊数が1冊以上ある。
4. 同じ会員が同じ本IDを現在借りていない。

貸出日には注入された`Clock`の現在日を使用し、返却期限を14日後に設定する。すべての検証と保存が成功した後で操作完了とする。

会員1人あたりの総貸出冊数には上限を設けない。同じタイトルでも本IDが異なれば別の本として貸出できる。

### 6.4 返却

- 返却対象は貸出IDで指定する。
- 対象のアクティブな貸出が存在する場合だけ返却できる。
- 返却成功後はアクティブな貸出一覧から削除する。
- 存在しない貸出IDや返却済み貸出IDの再返却は拒否する。

### 6.5 期限と延滞

- 貸出日から14日後を返却期限とする。
- 現在日が返却期限と同じ日は延滞ではない。
- `currentDate.isAfter(dueDate)`の場合だけ延滞とする。
- 日付はシステムのタイムゾーンを利用する。
- テストでは`Clock.fixed(...)`を注入し、実行日によらない結果にする。

### 6.6 失敗時の状態

- 検証に失敗した操作では、メモリ上の状態と保存済みデータを変更しない。
- 保存に失敗した操作では、メモリ上の状態を変更せず、成功として表示しない。
- GUIは例外を握りつぶさず、英語のエラーダイアログを表示する。

## 7. 公開Service API

メソッド名と戻り値は次の契約に固定する。以下のコードブロックは公開メソッドの署名を示し、メソッド本体を省略している。実装都合だけを理由に、この契約を担当者単独で変更しない。

### 7.1 BookService

```java
public final class BookService {
    public Book addBook(String id, String title, String genre, int totalCopies);
    public Book updateBook(String id, String title, String genre, int totalCopies);
    public void deleteBook(String id);
    public Optional<Book> findBookById(String id);
    public List<BookSummary> listBooks();
    public List<BookSummary> searchBooks(String query);
    public int availableCopies(String bookId);
}
```

`BookSummary`はGUI表示用の変更不能な値とする。

```java
public record BookSummary(
        String id,
        String title,
        String genre,
        int totalCopies,
        int loanedCopies,
        int availableCopies) {
}
```

一覧は本IDの昇順で返す。

### 7.2 MemberService

```java
public final class MemberService {
    public Member addMember(String id, String name);
    public Member updateMember(String id, String name);
    public void deleteMember(String id);
    public Optional<Member> findMemberById(String id);
    public List<Member> listMembers();
    public List<Member> searchMembers(String query);
}
```

一覧は会員IDの昇順で返す。

### 7.3 LoanService

```java
public final class LoanService {
    public Loan checkout(String bookId, String memberId);
    public void returnLoan(String loanId);
    public Optional<LoanDetails> findActiveLoanById(String loanId);
    public List<LoanDetails> listActiveLoans();
    public List<LoanDetails> findActiveLoansByBook(String bookId);
    public List<LoanDetails> findActiveLoansByMember(String memberId);
    public List<Member> findBorrowersByBook(String bookId);
    public List<Book> findBorrowedBooksByMember(String memberId);
}
```

`LoanDetails`はGUI表示用の変更不能な値とする。

```java
public record LoanDetails(
        String id,
        Book book,
        Member member,
        LocalDate checkoutDate,
        LocalDate dueDate,
        boolean overdue) {
}
```

貸出一覧は貸出日の昇順、同日の場合は貸出IDの昇順で返す。

### 7.4 Serviceの依存関係

- `BookService`は`BookRepository`と`LoanQuery`に依存する。
- `MemberService`は`MemberRepository`と`LoanQuery`に依存する。
- `LoanService`は`BookRepository`、`MemberRepository`、`LoanRepository`、`Clock`、貸出ID用`Supplier<String>`に依存する。
- 本番用の`LoanService`は、システムの`Clock`とUUID生成処理を使うコンストラクタを提供する。
- テスト用のコンストラクタでは、`Clock`と`Supplier<String>`を明示的に注入できるようにする。

`LoanQuery`は本・会員側が貸出モデルの内部実装へ依存しないための共有インターフェースとする。

```java
public interface LoanQuery {
    int countActiveLoansForBook(String bookId);
    boolean hasActiveLoanForMember(String memberId);
}
```

### 7.5 検索結果と失敗時の動作

- `findBookById`、`findMemberById`、`findActiveLoanById`は、対象が存在しない場合に`Optional.empty()`を返す。
- `list`と`search`で始まるメソッドは、該当データがない場合に空の変更不能な一覧を返す。`null`は返さない。
- `updateBook`、`deleteBook`、`availableCopies`へ存在しない本IDを渡した場合は`EntityNotFoundException`とする。
- `updateMember`、`deleteMember`へ存在しない会員IDを渡した場合は`EntityNotFoundException`とする。
- `checkout`へ存在しない本IDまたは会員IDを渡した場合は`EntityNotFoundException`とする。
- `returnLoan`へ存在しない貸出IDを渡した場合は`EntityNotFoundException`とする。
- `findActiveLoansByBook`と`findBorrowersByBook`へ存在しない本IDを渡した場合は`EntityNotFoundException`とする。
- `findActiveLoansByMember`と`findBorrowedBooksByMember`へ存在しない会員IDを渡した場合は`EntityNotFoundException`とする。
- `LoanQuery`は参照先の存在確認を行わず、該当する貸出がなければ`0`または`false`を返す。存在確認は各ServiceがRepositoryを使って先に行う。

## 8. Repository契約

Repositoryはモデル単位の保存、読込、検索を担当する。保存前に新しい一覧を組み立て、DataStoreへの書込みが成功した場合だけメモリ上の一覧を置き換える。

### 8.1 BookRepository

```java
public interface BookRepository {
    List<Book> findAll();
    Optional<Book> findById(String id);
    void save(Book book);
    void deleteById(String id);
}
```

### 8.2 MemberRepository

```java
public interface MemberRepository {
    List<Member> findAll();
    Optional<Member> findById(String id);
    void save(Member member);
    void deleteById(String id);
}
```

### 8.3 LoanRepository

```java
public interface LoanRepository extends LoanQuery {
    List<Loan> findAll();
    Optional<Loan> findById(String id);
    List<Loan> findByBookId(String bookId);
    List<Loan> findByMemberId(String memberId);
    boolean existsByBookIdAndMemberId(String bookId, String memberId);
    void save(Loan loan);
    void deleteById(String id);
}
```

Repositoryが返すモデルと一覧を変更しても、Repository内部の状態が変化してはならない。

## 9. DataStoreと保存形式

### 9.1 保存先

プロジェクトの実行ディレクトリを基準に、次のファイルを使用する。

```text
data/books.data
data/members.data
data/loans.data
```

`data/`と実行時データはGitおよび提出コードへ含めない。

### 9.2 DataStore API

```java
public interface DataStore {
    List<List<String>> read(String collectionName);
    void write(String collectionName, List<List<String>> records);
}
```

- コレクション名は`books`、`members`、`loans`だけを許可する。
- `FileDataStore`のコンストラクタで保存ディレクトリの`Path`を受け取る。
- `read`は復号済みのフィールド一覧を返す。
- `read`が返す外側と内側の一覧は変更不能とする。
- `write`は文字列フィールドの符号化、UTF-8書込み、一時ファイルからの置換を担当する。
- 型変換とレコードの項目数検証は各Repositoryが担当する。

### 9.3 ファイル形式

- 文字コードはUTF-8とする。
- 改行コードはLFとする。
- 1行目は`LIBRARY-DATA-V1`とする。
- 2行目以降を1レコードとする。
- 各文字列フィールドをUTF-8へ変換し、URL-safe Base64のpaddingなしで符号化する。
- 符号化後のフィールドをタブで区切る。
- 数値と日付も文字列へ変換してから同じ方式で符号化する。
- 空の一覧はヘッダー行だけのファイルとして保存する。
- 保存順は各モデルのID昇順とし、同じ状態から同じ内容を生成する。

この形式により、入力値にタブ、改行、区切り文字、Unicode文字が含まれていてもレコード構造を維持する。

### 9.4 安全な保存と読込

1. 保存対象と同じディレクトリへ`<name>.data.tmp`を書き込む。
2. 書込み完了後、`ATOMIC_MOVE`と`REPLACE_EXISTING`を指定して本ファイルへ移動する。
3. ファイルシステムが`ATOMIC_MOVE`をサポートしない場合は、`REPLACE_EXISTING`で置換する。
4. 置換に失敗した場合は`DataStoreException`を送出し、メモリ上の状態を変更しない。

本の追加・修正・削除は本ファイルだけ、会員の追加・修正・削除は会員ファイルだけ、貸出・返却は貸出ファイルだけを変更する。貸出中冊数を保存せず貸出データから算出するため、1回の業務操作で複数ファイルを更新しない。

読込時の規則は次のとおりとする。

- ファイルが存在しない場合は空の一覧を返す。
- ヘッダー、Base64、項目数、数値、日付、ID重複が不正な場合は`DataStoreException`を送出する。
- 1件でも不正なレコードがあれば部分的に読み込まず、起動を失敗させる。
- 貸出データの本ID・会員IDが存在しない場合も起動時に`DataStoreException`とする。
- 壊れた既存ファイルを自動的に空データで上書きしない。

## 10. 例外契約

業務上想定される失敗には、次の非検査例外を使用する。

```text
LibraryException
├─ ValidationException
├─ DuplicateIdException
├─ EntityNotFoundException
├─ OperationNotAllowedException
└─ DataStoreException
```

- `ValidationException`: 空文字、不正なID、不正な冊数など。
- `DuplicateIdException`: 本ID、会員ID、貸出IDの重複。
- `EntityNotFoundException`: 指定した本、会員、貸出が存在しない。
- `OperationNotAllowedException`: 貸出中の削除、在庫不足、重複貸出、貸出中冊数未満への変更など。
- `DataStoreException`: 保存・読込・データ形式・参照整合性の失敗。

例外メッセージは英語で、利用者が次の操作を判断できる内容にする。GUIは`LibraryException`を捕捉し、`JOptionPane.ERROR_MESSAGE`でメッセージを表示する。想定外の例外はログへ詳細を残し、GUIには`An unexpected error occurred.`と表示する。

## 11. GUI共有仕様

### 11.1 メイン画面

- ウィンドウタイトルは`Library System`とする。
- タブの順序は`Books`、`Members`、`Loans`とする。
- 初期サイズは1000 x 650ピクセルとする。
- 起動は`SwingUtilities.invokeLater(...)`を使用し、Event Dispatch Thread上で行う。
- 一覧テーブルは行単位選択とし、列の並べ替えを有効にする。
- 日付はISO形式の`yyyy-MM-dd`で表示する。

### 11.2 Booksタブ

- 列は`ID`、`Title`、`Genre`、`Total`、`Loaned`、`Available`とする。
- 操作は`Add`、`Edit`、`Delete`、`Search`、`Clear`、`Borrowers`とする。
- `Edit`ではIDを変更できない。
- `Borrowers`では選択した本を現在借りている会員を表示する。

### 11.3 Membersタブ

- 列は`ID`、`Name`、`Borrowed`とする。
- 操作は`Add`、`Edit`、`Delete`、`Search`、`Clear`、`Borrowed Books`とする。
- `Edit`ではIDを変更できない。
- `Borrowed Books`では選択した会員が現在借りている本を表示する。

### 11.4 Loansタブ

- 列は`Loan ID`、`Book`、`Member`、`Checkout Date`、`Due Date`、`Status`とする。
- 操作は`Checkout`と`Return`とする。
- `Status`は`On time`または`Overdue`と表示する。
- `Checkout`では本と会員を一覧から選択する。
- 貸出ダイアログには利用可能冊数が1冊以上ある本だけを表示する。
- 貸出可能な本がない場合は確定ボタンを無効にし、`No books are available for checkout.`と表示する。
- `Return`は選択した貸出を対象とする。

### 11.5 画面更新の契約

各パネルは次のメソッドを公開する。

```java
public void refreshData();
```

各パネルと`MainFrame`のコンストラクタ契約は次のとおりとする。

```java
public MainFrame(
        BookService bookService,
        MemberService memberService,
        LoanService loanService);

public BookPanel(
        BookService bookService,
        LoanService loanService,
        Runnable dataChanged);

public MemberPanel(
        MemberService memberService,
        LoanService loanService,
        Runnable dataChanged);

public LoanPanel(
        BookService bookService,
        MemberService memberService,
        LoanService loanService,
        Runnable dataChanged);
```

`MainFrame`は`dataChanged`コールバックを使い、追加、修正、削除、貸出、返却の成功後に3つのパネルすべてへ`refreshData()`を呼び出す。

失敗した操作では`dataChanged`を呼び出さない。

## 12. 起動処理

`Main`は次の順にオブジェクトを構築する。

1. 保存先`Path`と`FileDataStore`。
2. `BookRepository`、`MemberRepository`、`LoanRepository`。
3. `LibraryDataValidator`による貸出データの参照整合性検証。
4. `BookService`、`MemberService`、`LoanService`。
5. `MainFrame`と各パネル。

読込または参照整合性検証に失敗した場合、空データで起動せず、英語のエラーダイアログを表示して終了する。

参照整合性検証の公開契約は次のとおりとする。

```java
public final class LibraryDataValidator {
    public static void validate(
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LoanRepository loanRepository);
}
```

検証では、すべての貸出について本と会員が存在すること、同じ会員と本IDの組合せが重複していないこと、本ごとの貸出件数が所有冊数以下であることを確認する。

## 13. 担当境界

### ryota

- `Book`、`BookService`、`BookSummary`。
- `BookRepository`と本データのRepository実装。
- `DataStore`、`FileDataStore`、`LoanQuery`、例外階層、共通ファイルI/O。
- `Main`、`MainFrame`、`BookPanel`、`BookDialog`。
- 本、在庫、本データ永続化のJUnitテスト。

### kumpei

- `Member`、`Loan`、`MemberService`、`LoanService`、`LoanDetails`。
- `MemberRepository`、`LoanRepository`と`LoanQuery`の実装。
- 会員・貸出データのRepository実装と参照整合性検証。
- `MemberPanel`、`MemberDialog`、`LoanPanel`、`CheckoutDialog`。
- 会員、貸出返却、照会、期限のJUnitテスト。

### 共有変更

- 公開Service API、Repository API、例外階層、保存形式、GUIのパネル連携は共有契約とする。
- 同じファイルを同時に編集しない。
- 他担当の公開APIに変更が必要な場合は、実装前に理由と変更内容を共有する。
- `MainFrame`へのMembers・Loansタブの組込みは、kumpeiのパネルAPI確定後にryotaが行う。

## 14. Gitとレビュー

- ryotaは`feature/ryota-books`、kumpeiは`feature/kumpei-circulation`で作業を開始する。
- 公開契約とテスト骨格ができた時点でDraft PRを作成する。
- 1コミットを1つの意味のある変更に限定する。
- Pull Requestには担当範囲、確認方法、テスト結果、未完了項目を記載する。
- 共有APIの変更は相手の承認後に実装する。
- Ready for reviewにする前に、本体コンパイル、担当JUnit、実装内の日本語混入を確認する。
- mainへのマージ前に、作成者以外がレビューし、未解決コメントをなくす。
- 分担の証拠となる履歴を残すため、原則としてSquash mergeを使用しない。

## 15. 共有完了条件

- 本・会員のCRUD、貸出・返却、双方向の照会をGUIから実行できる。
- 所有冊数、貸出中冊数、利用可能冊数が常に一致する。
- 再起動後も本、会員、貸出が復元される。
- 在庫不足、重複貸出、貸出中の削除、不正入力を拒否する。
- 保存失敗や検証失敗の後に状態が変化していない。
- 全JUnitテストが成功する。
- `src/main/java`をJava SEだけでコンパイルできる。
- 実装全体に日本語が含まれていない。
- `.class`、JUnit JAR、実行時データを提出物へ含めない。
