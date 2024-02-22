# Webview

The Java port of the [webview project](https://github.com/webview/webview). It uses JNA and auto-extracts the required dll/dylib/so libraries for your current system.

<div align="center">
    <img alt="browser" src="demo.png" width="50%" />
    <br />
    <br />
</div>

## Examples

[Example](https://github.com/webview/webview_java/blob/main/core/src/test/java/com/example/webview_java/Example.java)  
[Bridge Example](https://github.com/webview/webview_java/blob/main/bridge/src/test/java/com/example/webview_java/BridgeExample.java)

## Supported Platforms

<table width=300>
    <tr>
        <td align="right" width=64>
            <img src="https://simpleicons.org/icons/windows.svg" title="Windows" width="32" height="32">
        </td>
        <td align="left">
            x86, x86_64
        </td>
    </tr>
    <tr>
        <td align="right" width=64>
            <img src="https://simpleicons.org/icons/apple.svg" title="macOS" width="32" height="32">
        </td>
        <td align="left">
            aarch64, x86_64
        </td>
    </tr>
    <tr>
        <td align="right" width=64>
            <img src="https://simpleicons.org/icons/linux.svg" title="Linux" width="32" height="32">
        </td>
        <td align="left">
            x86, x86_64, arm, aarch64
        </td>
    </tr>
</table>

### Linux
Both MUSL and GLibC are supported out of the box. So it should work fine under distros like Debian, Arch, and Alpine.

### macOS

macOS requires that all UI code be executed from the first thread, which means you will need to launch Java with `-XstartOnFirstThread`. This also means that the Webview AWT helper will NOT work at all.

## Getting the code

[![](https://jitpack.io/v/webview/webview_java.svg)](https://jitpack.io/#webview/webview_java)

Replace `_VERSION` with the latest version or commit in this repo. If you want the Bridge bindings you'll need both `core` and `bridge`.

<details>
  <summary>Maven</summary>
  
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.webview.webview_java</groupId>
    <artifactId>core</artifactId>
    <version>_VERSION</version>
</dependency>
```
</details>

<details>
<summary>Gradle</summary>

```gradle
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  implementation 'com.github.webview.webview_java:core:_VERSION'
}
```

</details>

<details>
  <summary>SBT</summary>

```sbt
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.webview.webview_java" % "core" % "\_VERSION"
```

</details>

<details>
<summary>Leiningen</summary>

```lein
:repositories [["jitpack" "https://jitpack.io"]]

:dependencies [[com.github.webview.webview_java/core "_VERSION"]]
```

</details>
