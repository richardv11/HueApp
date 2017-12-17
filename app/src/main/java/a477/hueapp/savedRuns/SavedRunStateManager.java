package a477.hueapp.savedRuns;

public class SavedRunStateManager {

    private static SavedRunStateManager instance;

    private SavedRunStates playerState;
    private int lastNoteIndex;
    private Thread runThread;

    private SavedRunStateManager(){
        playerState = SavedRunStates.STOPPED;
        lastNoteIndex = 0;
    }

    public static SavedRunStateManager getInstance(){
        if(instance == null)
            instance = new SavedRunStateManager();
        return instance;
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

    public void setRunThread(Thread runThread){
        this.runThread = runThread;
    }

    public void pauseThread(){
        if(playerState.equals(SavedRunStates.PLAYING)) {
            runThread.interrupt();
            playerPaused();
        }
    }

    public void stopThread(){
        if(playerState.equals(SavedRunStates.PLAYING)) {
            if (runThread != null)
                runThread.interrupt();
            playerStopped();
        }
    }
}
