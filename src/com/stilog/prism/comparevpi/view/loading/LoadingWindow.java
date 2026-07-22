package com.stilog.prism.comparevpi.view.loading;

import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class LoadingWindow {

	public static void run(JFrame parent, Runnable task) {
        run(parent, task, "Chargement...");
    }

    public static void run(JFrame parent, Runnable task, String message) {

        LoadingDialog dialog = new LoadingDialog(parent, message);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();  // <-- ton code lourd ici
                return null;
            }

            @Override
            protected void done() {
                dialog.dispose();
                // get() relance toute exception survenue dans task.run() : sans ce
                // contrôle, doInBackground() pouvait échouer sans que rien ne le
                // signale à l'utilisateur (fenêtre de chargement qui se ferme, silence).
                try {
                    get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    cause.printStackTrace();
                    JOptionPane.showMessageDialog(parent,
                        cause.getMessage() != null ? cause.getMessage() : cause.toString(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        worker.execute();
        dialog.setVisible(true); // bloque l’UI mais ne freeze pas
    }
}
