package com.hci.sroettering.spotifyrc.wiigee.logic;

import java.util.Vector;

import com.hci.sroettering.spotifyrc.wiigee.event.AccelerationEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.ButtonPressedEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.ButtonReleasedEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.AccelerationListener;
import com.hci.sroettering.spotifyrc.wiigee.event.ButtonListener;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.GestureListener;
import com.hci.sroettering.spotifyrc.wiigee.event.MotionStartEvent;
import com.hci.sroettering.spotifyrc.wiigee.event.MotionStopEvent;
import com.hci.sroettering.spotifyrc.wiigee.util.Log;

public abstract class ProcessingUnit implements AccelerationListener, ButtonListener {

    // Classifier
    protected Classifier classifier;
    
    // Listener
    private Vector<GestureListener> gesturelistener = new Vector<GestureListener>();

    public ProcessingUnit() {
        this.classifier = new Classifier();
    }

    /**
     * Add an GestureListener to receive GestureEvents.
     *
     * @param g
     * 	Class which implements GestureListener interface.
     */
    public void addGestureListener(GestureListener g) {
        this.gesturelistener.add(g);
    }

    protected void fireGestureEvent(boolean valid, int id, double probability) {
        GestureEvent w = new GestureEvent(this, valid, id, probability);
        for (int i = 0; i < this.gesturelistener.size(); i++) {
            this.gesturelistener.get(i).gestureReceived(w);
        }
    }

    public abstract void accelerationReceived(AccelerationEvent event);

    public abstract void buttonPressReceived(ButtonPressedEvent event);

    public abstract void buttonReleaseReceived(ButtonReleasedEvent event);

    public abstract void motionStartReceived(MotionStartEvent event);

    public abstract void motionStopReceived(MotionStopEvent event);

    /**
     * Resets the complete gesturemodel. After reset no gesture is known
     * to the system.
     */
    public void reset() {
        if (this.classifier.getCountOfGestures() > 0) {
            this.classifier.clear();
            Log.write("### Model reset ###");
        } else {
            Log.write("There doesn't exist any data to reset.");
        }
    }

    // File IO
    public abstract void loadGesture(String filename);

    public abstract void saveGesture(int id, String filename);
}
