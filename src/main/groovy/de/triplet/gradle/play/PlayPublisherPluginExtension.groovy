package de.triplet.gradle.play

class PlayPublisherPluginExtension {

    String serviceAccountEmail

    File pk12File

    File jsonFile

    File obbMain

    File obbPatch

    boolean uploadImages = false

    int associateObbMain = -1

    int associateObbPatch = -1

    boolean errorOnSizeLimit = true

    private String track = 'alpha'

    boolean untrackOld = false

    void setTrack(String track) {
        if (!(track in ['alpha', 'beta', 'rollout', 'production'])) {
            throw new IllegalArgumentException("Track has to be one of 'alpha', 'beta', 'rollout' or 'production'.")
        }

        this.track = track
    }

    def getTrack() {
        return track
    }

    Double userFraction = 0.1

}
