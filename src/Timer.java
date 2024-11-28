/**
 * Classe utilitaire pour gérer les temporisations (timeouts) personnalisées.
 * Permet de définir une action à exécuter lorsqu'un timeout survient.
 * Supporte le démarrage, l'arrêt et le redémarrage du timer.
 */
public class Timer {
    /**
     * Durée du timeout en millisecondes.
     */
    private int timeout;

    /**
     * Thread utilisé pour gérer le timeout.
     */
    private Thread timerThread;

    /**
     * Indique si le timer est actuellement en cours d'exécution.
     */
    private boolean isRunning;

    /**
     * Handler (action) à exécuter lors d'un timeout.
     */
    private Runnable timeoutHandler;

    /**
     * Constructeur par défaut du Timer.
     *
     * @param timeout La durée du timeout en millisecondes.
     */
    public Timer(int timeout) {
        this.timeout = timeout;
        this.isRunning = false;
    }

    /**
     * Définit l'action à exécuter lorsqu'un timeout survient.
     *
     * @param handler Un objet Runnable représentant l'action à exécuter.
     */
    public void setTimeoutHandler(Runnable handler) {
        this.timeoutHandler = handler;
    }

    /**
     * Démarre le timer. Si un timer est déjà en cours, il est arrêté avant de démarrer un nouveau.
     * Après la durée spécifiée, l'action définie par {@link #setTimeoutHandler(Runnable)} sera exécutée.
     */
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

    /**
     * Arrête le timer en cours d'exécution. Si aucun timer n'est en cours, cette méthode n'a aucun effet.
     * Interrompt le thread du timer et le met à null.
     */
    public synchronized void stop() {
        isRunning = false;
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    /**
     * Redémarre le timer en arrêtant le timer actuel (s'il est en cours) et en démarrant un nouveau timer.
     * Après la durée spécifiée, l'action définie par {@link #setTimeoutHandler(Runnable)} sera exécutée.
     */
    public synchronized void restart() {
        stop();
        start();
    }

    /**
     * Vérifie si le timer est actuellement en cours d'exécution.
     *
     * @return {@code true} si le timer est en cours d'exécution, {@code false} sinon.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Obtient la durée du timeout.
     *
     * @return La durée du timeout en millisecondes.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Modifie la durée du timeout.
     * Si le timer est en cours d'exécution, il sera redémarré avec la nouvelle durée.
     *
     * @param timeout La nouvelle durée du timeout en millisecondes.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
        if (isRunning) {
            restart();
        }
    }
}
