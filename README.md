# Webview

A (new!) Java port of the [webview project](https://github.com/webview/webview). It uses JNA under the hood to interface with the webview library.

## How to use

1) Include the libary in your project (see the [JitPack page](https://jitpack.io/#Casterlabs/webview)).
2) Copy and run the example in `Example.java`.
3) Profit!

## MacOS
MacOS requires that all UI code be executed from the first thread, which means you will need to launch Java with `-XstartOnFirstThread`. This also means that the Webview AWT helper will NOT work at all.

## TODO

Build our own DLLs and whatnot, the current ones are copied from the C# port.