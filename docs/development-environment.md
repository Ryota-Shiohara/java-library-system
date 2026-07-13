# 開発環境・コンパイル・JUnit手順

## 方針

このプロジェクトはMavenやGradleを使わず、アプリケーション本体をJava SEだけでコンパイルする。JUnitはテスト専用の外部ライブラリとして、JUnit Platform Console Standalone 6.1.1を使用する。

現在の標準はJDK 17以上とし、`javac --release 17`でコンパイルする。JDK 21を使っていても、Java 17で動くコードとして確認できる。

## 初回セットアップ

1. VS Codeに`Extension Pack for Java`をインストールする。
2. プロジェクトのルートフォルダをVS Codeで開く。
3. PowerShellで次を実行し、JUnit JARを取得する。

```powershell
New-Item -ItemType Directory -Force lib | Out-Null
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.1/junit-platform-console-standalone-6.1.1.jar" `
  -OutFile "lib\junit-platform-console-standalone-6.1.1.jar"
```

4. `java -version`と`javac -version`を実行し、JDKが使用できることを確認する。

JUnit JARは`.gitignore`で除外されているため、Gitには追加しない。別のPCで作業するときは、そのPCでも同じJARを`lib`に配置する。

## VS Codeでの操作

- `Ctrl+Shift+B`: 本体コードをコンパイルする。
- コマンドパレットの`Tasks: Run Task`から`Compile test sources`: テストコードをコンパイルする。
- コマンドパレットの`Tasks: Run Task`から`Run all JUnit tests`: 全JUnitテストをコンパイルして実行する。
- テストの緑色の実行ボタンやTestingビューは、個別テストの実行・デバッグに利用できる。提出前の確認は`Run all JUnit tests`タスクを基準にする。

Java拡張機能の言語サーバーが作るIDE用のクラスファイルは`out/ide`に置く。手動コンパイルの結果とは分離している。

## PowerShellからの操作

本体だけをコンパイルする。

```powershell
.\scripts\compile.ps1 -Target main
```

本体とテストをコンパイルする。

```powershell
.\scripts\compile.ps1 -Target all
```

テストをコンパイル済みクラスから実行する。

```powershell
.\scripts\test.ps1 -NoBuild
```

コンパイルからテスト実行まで一度に行う。

```powershell
.\scripts\test.ps1
```

## 生成物の場所

| 生成物 | 場所 | Git・提出物への扱い |
|---|---|---|
| 本体の`.class` | `out/main/` | Git・提出物に含めない |
| テストの`.class` | `out/test/` | Git・提出物に含めない |
| VS Code言語サーバーの出力 | `out/ide/` | Git・提出物に含めない |
| JUnit実行レポート | `out/test-reports/` | Git・提出物に含めない |
| 実行時データ | `data/` | Git・提出物に含めない |
| 提出用の一時配置・ZIP | `output/` | ソースとREADMEなど必要なものだけ含める |
| JUnit JAR | `lib/junit-platform-console-standalone-6.1.1.jar` | Git・提出物に含めない |

`src`以下に`.class`を生成しないこと。提出前は、`src`、必要なスクリプト、README、レポートなどを`output`へコピーし、`out`、`data`、`lib`を混入させない。

## コマンドの意味

本体コンパイルではJUnitのクラスパスを指定しない。これにより、本体がJava SEだけで完結していることを確認できる。

テストコンパイルでは、本体のクラスとJUnit JARをクラスパスに追加する。

JUnit実行では、本体クラスとテストクラスだけをクラスパスに指定し、`--scan-class-path`でテストを自動検出する。テストが1件も見つからない場合も成功扱いにしないため、`--fail-if-no-tests`を指定している。

## GitHub Actions CI

`.github/workflows/ci.yml`で、`main`へのPush、`main`向けPull Request、手動実行を検証する。

CIの処理は次のとおり。

1. Ubuntu上にJDK 21を用意する。
2. `src/main/java`にJavaファイルがある場合、JUnit JARを取得する。
3. `scripts/test.ps1`で本体とテストをコンパイルし、JUnitを実行する。
4. `out/test-reports/`のJUnitレポートをGitHub ActionsのArtifactとして保存する。

Javaソースがまだない初期状態では、CIをスキップして成功扱いにする。本体ソースが追加された後は、テストソースがない場合やテストが失敗した場合にCIを失敗させる。
