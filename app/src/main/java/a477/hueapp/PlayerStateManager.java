package a477.hueapp;

public class PlayerStateManager {

    private static PlayerStateManager instance;

    private PlayerState playerState;
    private Thread mainPlayerThread;

    private PlayerStateManager() {
        playerState = PlayerState.STOPPED;
    }

    public static PlayerStateManager getInstance() {
        if (instance == null)
            instance = new PlayerStateManager();
        return instance;
    }

    public synchronized void playerStarted() {
        playerState = PlayerState.PLAYING;
    }

    public synchronized void playerPaused() {
        playerState = PlayerState.PAUSED;
    }

    public synchronized void playerStopped() {
        playerState = PlayerState.STOPPED;
    }

    public PlayerState getState() {
        return playerState;
    }

    public synchronized void stopPlayer() {
        if (PlayerStateManager.getInstance().getState().equals(PlayerState.PLAYING)) {
            playerStopped();
        }
    }

    public synchronized void setMainPlayerThread(Thread mainPlayerThread) {
        this.mainPlayerThread = mainPlayerThread;
    }

    public Thread getMainPlayerThread(){
        return this.mainPlayerThread;
    }
}
