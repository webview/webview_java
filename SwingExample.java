package dev.webview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class SwingExample {

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Using createAWT allows you to defer the creation of the webview until the
        // canvas is fully renderable.
        Component component = Webview.createAWT((wv) -> {
            // Calling `await echo(1,2,3)` will return `[1,2,3]`
            wv.bind("echo", (arguments) -> {
                return arguments;
            });

            wv.loadURL("https://google.com");

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    wv.close();
                    frame.dispose();
                    System.exit(0);
                }
            });

            // Run the webview event loop, the webview is fully disposed when this returns.
            wv.run();
        });

        frame.getContentPane().add(component, BorderLayout.CENTER);

        frame.setSize(800, 600);
        frame.setVisible(true);
    }

}
