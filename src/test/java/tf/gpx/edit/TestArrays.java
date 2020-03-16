/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.main;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 *
 * @author thomas
 */
public class TestArrays extends Application {
    Button btn_js;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        WebView myWebView = new WebView();

        myWebView.getEngine().loadContent("<html>\n" 
                        + " <script>function printArray(arr) {" 
                        + "for (i = 0; i < arr.length; i++) {"
                        + " window.alert(arr[i]);" 
                        + "}"
                        + "} </script> " 
                        + "    </body>\n" 
                        + "</html>");

        List<String> arr = new ArrayList<String>();
        arr.add("Dog");
        arr.add("Cat");

        btn_js = new Button("fire js");
        btn_js.setOnAction(e -> myWebView.getEngine().executeScript("printArray(" + transformToJavascriptArray(arr) + ")"));

        myWebView.getEngine().setOnAlert(event -> System.out.println("JS alert: " + event.getData()));

        VBox root = new VBox();
        root.getChildren().addAll(myWebView, btn_js);
        Scene scene = new Scene(root, 500, 500);
        stage.setScene(scene);
        stage.show();
    }

    private String transformToJavascriptArray(List<String> arr) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");

        for (String str : arr)
            sb.append("\"").append(str).append("\"").append(", ");

        if (sb.length() > 1)
            sb.replace(sb.length() - 2, sb.length(), "");

        sb.append("]");

        return sb.toString();
    }
}
