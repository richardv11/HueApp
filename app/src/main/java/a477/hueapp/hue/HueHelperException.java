package a477.hueapp.hue;

class HueHelperException extends Exception {

    String errorMsg;

    public HueHelperException(String msg){
        errorMsg=msg;
    }

    @Override
    public String toString(){
        return errorMsg;
    }
}
