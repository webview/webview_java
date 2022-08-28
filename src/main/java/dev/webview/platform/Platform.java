package dev.webview.platform;

public class Platform {
    public static final Arch arch;
    public static final OperatingSystem os;

    static {
        arch = Arch.get();
        os = OperatingSystem.get();
    }

}
