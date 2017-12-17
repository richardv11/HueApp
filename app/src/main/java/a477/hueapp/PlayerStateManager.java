package a477.hueapp;

public class PlayerStateManager {

    private static PlayerStateManager instance;

    private PlayerState playerState;

    private PlayerStateManager() {
        playerState = PlayerState.STOPPED;
    }

    public static PlayerStateManager getInstance() {
        if (instance == null)
            instance = new PlayerStateManager();
        return instance;
    }

    public void playerStarted() {
        playerState = PlayerState.PLAYING;
    }

    public void playerPaused() {
        playerState = PlayerState.PAUSED;
    }

    public void playerStopped() {
        playerState = PlayerState.STOPPED;
    }

    public PlayerState getState() {
        return playerState;
    }

    public void stopPlayer() {
        if (PlayerStateManager.getInstance().getState().equals(PlayerState.PLAYING)) {
            playerStopped();
        }
    }
}
