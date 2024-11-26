public class Timer {
    private int timeout;  // Durée du timeout en millisecondes
    private Thread timerThread;
    private boolean isRunning;
    private Runnable timeoutHandler;  // Handler à exécuter lors d'un timeout

    public Timer(int timeout) {
        this.timeout = timeout;
        this.isRunning = false;
    }

    // Définir l'action à exécuter lors d'un timeout
    public void setTimeoutHandler(Runnable handler) {
        this.timeoutHandler = handler;
    }

    // Démarrer le timer
    public synchronized void start() {
        if (isRunning) {
            stop();  // Arrêter le timer existant si déjà en cours
        }

        isRunning = true;
        timerThread = new Thread(() -> {
            try {
                Thread.sleep(timeout);
                if (isRunning && timeoutHandler != null) {
                    timeoutHandler.run();
                }
            } catch (InterruptedException e) {
                // Timer interrompu, c'est normal lors d'un stop()
            }
        });
        timerThread.start();
    }

    // Arrêter le timer
    public synchronized void stop() {
        isRunning = false;
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    // Redémarrer le timer
    public synchronized void restart() {
        stop();
        start();
    }

    // Vérifier si le timer est en cours d'exécution
    public boolean isRunning() {
        return isRunning;
    }

    // Obtenir la durée du timeout
    public int getTimeout() {
        return timeout;
    }

    // Modifier la durée du timeout
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}