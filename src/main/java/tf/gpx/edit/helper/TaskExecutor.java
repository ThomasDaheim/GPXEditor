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
package tf.gpx.edit.helper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author thomas
 */
public class TaskExecutor {
    private final static TaskExecutor INSTANCE = new TaskExecutor();
    
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);
    
    private TaskExecutor() {
        super();
        // Exists only to defeat instantiation.
    }

    public static TaskExecutor getInstance() {
        return INSTANCE;
    }
    
    public static void shutDown() {
        // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            } 
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
        }        
    }
    
    public static void executeTask(final Scene scene, final Runnable runnable, final ITaskExecutionConsumer consumer) {
        if (Platform.isFxApplicationThread()) {
            final Task<Void> task = taskFromRunnableForLater(scene, runnable);
            
            // bind properties
            final ChangeListener<String> messageListener = (ObservableValue<? extends String> ov, String oldValue, String newValue) -> {
                consumer.getMessageConsumer().accept(newValue);
            };
            task.messageProperty().addListener(messageListener);

            final ChangeListener<Number> progressListener = (ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                consumer.getProgressConsumer().accept(newValue);
            };
            task.progressProperty().addListener(progressListener);

            scene.setCursor(Cursor.WAIT); //Change cursor to wait style
            // schedule task
            consumer.getInitTaskConsumer().run();
            Future<Void> future = ObjectsHelper.uncheckedCast(executorService.submit(task));

            // wait for result
            Void result = null;
            try {
                result = future.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TaskExecutor.class.getName()).log(Level.SEVERE, null, ex);
            }        
            scene.setCursor(Cursor.DEFAULT);

            // unbind properties
            task.messageProperty().removeListener(messageListener);
            task.progressProperty().removeListener(progressListener);

            // "return" result
            consumer.getFinalizeTaskConsumer().accept(result);
        } else {
            // not running online - no need to wait for anything
            runnable.run();
        }
    }
    
    private static Task<Void> taskFromRunnableForLater(final Scene scene, final Runnable runnable) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (scene != null) {
                    Platform.runLater(() -> {
                        scene.setCursor(Cursor.WAIT); //Change cursor to wait style
                        runnable.run();
                        scene.setCursor(Cursor.DEFAULT);
                    });
                } else {
                    runnable.run();
                }

                return (Void) null;
            }
        };
    }
}
