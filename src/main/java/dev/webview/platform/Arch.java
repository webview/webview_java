package dev.webview.platform;

import java.util.regex.Pattern;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Arch {
    // @formatter:off
    AMD64    ("amd64",    "amd64|x86_64"),
    X86      ("x86",      "x86|i386|i486|i586|i686|i786"),
    AARCH64  ("aarch64",  "arm64|aarch64"),
    ARM32    ("arm32",    "arm");
    // @formatter:on

    private String str;
    private String regex;

    static Arch get() {
        String osArch = System.getProperty("os.arch").toLowerCase();

        for (Arch arch : values()) {
            if (Pattern.compile(arch.regex).matcher(osArch).find()) {
                return arch;
            }
        }

        throw new UnsupportedOperationException("Unknown cpu arch: " + osArch);
    }

    @Override
    public String toString() {
        return this.str;
    }

}
