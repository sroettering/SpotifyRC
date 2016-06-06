package com.hci.sroettering.spotifyrc.wiigee.event;

import com.hci.sroettering.spotifyrc.wiigee.logic.ProcessingUnit;
import com.hci.sroettering.spotifyrc.wiigee.logic.TriggeredProcessingUnit;

/**
 * Created by sroettering on 06.06.16.
 */
public class GestureTrainedEvent {

    private int id;

    private TriggeredProcessingUnit source;

    public GestureTrainedEvent(int id, TriggeredProcessingUnit src) {
        this.id = id;
        this.source = src;
    }

    public int getID() {
        return id;
    }

    public TriggeredProcessingUnit getSource() {
        return source;
    }

}
