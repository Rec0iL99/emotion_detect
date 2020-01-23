package com.semicolon.emotiondetect;

import android.graphics.Bitmap;

public class Reaction {
    private String videoTitle;
    private String videoTime;
    private String videoReaction;
    private String videoThumb;

    public Reaction(String videoTitle, String videoTime, String videoReaction, String videoThumb) {
        this.videoTitle = videoTitle;
        this.videoTime = videoTime;
        this.videoReaction = videoReaction;
        this.videoThumb = videoThumb;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getVideoTime() {
        return videoTime;
    }

    public void setVideoTime(String videoTime) {
        this.videoTime = videoTime;
    }

    public String getVideoReaction() {
        return videoReaction;
    }

    public void setVideoReaction(String videoReaction) {
        this.videoReaction = videoReaction;
    }

    public String getVideoThumb() {
        return videoThumb;
    }

    public void setVideoThumb(String videoThumb) {
        this.videoThumb = videoThumb;
    }

}
