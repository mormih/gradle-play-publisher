package de.triplet.gradle.play

import com.android.build.gradle.api.ApkVariantOutput
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApkListing
import com.google.api.services.androidpublisher.model.ExpansionFile
import com.google.api.services.androidpublisher.model.Track
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class PlayPublishApkTask extends PlayPublishTask {

    static def MIME_TYPE_APK = "application/vnd.android.package-archive"
    static def MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT = 500
    static def FILE_NAME_FOR_WHATS_NEW_TEXT = "whatsnew"

    File inputFolder

    @TaskAction
    publishApks() {
        super.publish()

        List<Integer> versionCodes = new ArrayList<Integer>()

        variant.outputs
                .findAll { variantOutput -> variantOutput instanceof ApkVariantOutput }
                .each { variantOutput -> versionCodes.add(publishApk(new FileContent(MIME_TYPE_APK, variantOutput.outputFile)).getVersionCode()) }

        Track track = new Track().setVersionCodes(versionCodes)
        if (extension.track?.equals("rollout")) {
            track.setUserFraction(extension.userFraction)
        }
        edits.tracks()
                .update(variant.applicationId, editId, extension.track, track)
                .execute()

        edits.commit(variant.applicationId, editId).execute()
    }

    def Apk publishApk(apkFile) {

        Apk apk = edits.apks()
                .upload(variant.applicationId, editId, apkFile)
                .execute()

        if (extension.untrackOld && !"alpha".equals(extension.track)) {
            def untrackChannels = "beta".equals(extension.track) ? ["alpha"] : ["alpha", "beta"]
            untrackChannels.each { channel ->
                try {
                    Track track = edits.tracks().get(variant.applicationId, editId, channel).execute()
                    track.setVersionCodes(track.getVersionCodes().findAll {
                        it > apk.getVersionCode()
                    });

                    edits.tracks().update(variant.applicationId, editId, channel, track).execute()
                } catch (GoogleJsonResponseException e) {
                    // Just skip if there is no version in track
                    if (e.details.getCode() != 404) {
                        throw e;
                    }
                }
            }
        }

        //Upload Proguard mapping.txt if available
        if (variant.mappingFile != null && variant.mappingFile.exists()) {
            AbstractInputStreamContent fileAsStream = new FileContent(AndroidPublisherHelper.MIME_TYPE_OCTET_STREAM, variant.mappingFile)
            edits.deobfuscationfiles().upload(variant.applicationId, editId, apk.getVersionCode(), "proguard", fileAsStream).execute()
        }

        if (inputFolder.exists()) {

            // Matches if locale have the correct naming e.g. en-US for play store
            inputFolder.eachDirMatch(matcher) { dir ->
                File whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT + "-" + extension.track)

                if (!whatsNewFile.exists()) {
                    whatsNewFile = new File(dir, FILE_NAME_FOR_WHATS_NEW_TEXT)
                }

                if (whatsNewFile.exists()) {

                    def whatsNewText = TaskHelper.readAndTrimFile(whatsNewFile, MAX_CHARACTER_LENGTH_FOR_WHATS_NEW_TEXT, extension.errorOnSizeLimit)
                    def locale = dir.name

                    ApkListing newApkListing = new ApkListing().setRecentChanges(whatsNewText)
                    edits.apklistings()
                            .update(variant.applicationId, editId, apk.getVersionCode(), locale, newApkListing)
                            .execute()
                }
            }

        }

        if (extension.obbMain != null && extension.obbMain.exists()) {
            publishObb("main", extension.obbMain)
        } else if (extension.associateObbMain > 0) {
            associateObb("main", extension.associateObbMain)
        }

        if (extension.uploadObbPatch) {
            publishObb("patch", extension.obbPatchPath)
        } else if (extension.associateObbPatch > 0) {
            associateObb("patch", extension.associateObbPatch)
        }

        edits.commit(variant.applicationId, editId).execute()
    }

    private void publishObb(String type, obbFile) {

        if (obbFile != null && obbFile.exists()) {
            def newObbFile = new FileContent("application/octet-stream", obbFile)

            edits.expansionfiles()
                    .upload(variant.applicationId, editId, variant.versionCode, type, newObbFile)
                    .execute()

            logger.info("Starting upload of the obb file ({} MB), this may take a while",
                    obbFile.length() / 1024 / 1024)

        } else {
            throw new GradleException("Please place a file named `${type}` in the `play/obb/` directory")
        }
    }

    private void associateObb(String type, int version) {
        ExpansionFile content = new ExpansionFile()
        content.setReferencesVersion(version)
        edits.expansionfiles()
                .update(variant.applicationId, editId, variant.versionCode, type, content)
                .execute()
    }

}
