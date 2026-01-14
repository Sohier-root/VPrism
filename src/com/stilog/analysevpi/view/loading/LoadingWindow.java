package com.stilog.analysevpi.view.loading;

import javax.swing.JFrame;
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
            }
        };

        worker.execute();
        dialog.setVisible(true); // bloque l’UI mais ne freeze pas
    }
}
