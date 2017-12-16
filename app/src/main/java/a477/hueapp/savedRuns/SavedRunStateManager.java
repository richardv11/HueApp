package a477.hueapp.savedRuns;

/**
 * Created by mrodger4 on 12/16/17.
 */

public class SavedRunStateManager {

    private static SavedRunStateManager instance;

    private SavedRunStates playerState;
    private int lastNoteIndex;

    private SavedRunStateManager(){
        playerState = SavedRunStates.STOPPED;
        lastNoteIndex = 0;
    }

    public static SavedRunStateManager getInstance(){
        if(instance == null)
            instance = new SavedRunStateManager();
        return instance;
    }

    private SavedRunStates getPlayerState(){
        return playerState;
    }

    public void playerStarted(){
        playerState = SavedRunStates.PLAYING;
    }

    public void playerPaused(){
        playerState = SavedRunStates.PAUSED;
    }

    public void playerStopped(){
        playerState = SavedRunStates.STOPPED;
        lastNoteIndex = 0;
    }

    public SavedRunStates getState(){
        return playerState;
    }

    public void setLastNoteIndex(int idx){
        lastNoteIndex = idx;
    }

    public int getLastNoteIndex(){
        return lastNoteIndex;
    }
}
