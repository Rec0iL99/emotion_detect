package com.semicolon.emotiondetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ReactionListAdapter extends ArrayAdapter<Reaction> {

    private static final String TAG = "ReactionListAdapter";
    private Context context;
    private int resource;

    public ReactionListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Reaction> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String videoTitle = getItem(position).getVideoTitle();
        String videoTime = getItem(position).getVideoTime();
        String videoReaction = getItem(position).getVideoReaction();
        String videoThumb = getItem(position).getVideoThumb();

        Reaction reaction = new Reaction(videoTitle, videoTime, videoReaction, videoThumb);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        convertView = layoutInflater.inflate(resource, parent, false);

        TextView videoTitleText = convertView.findViewById(R.id.video_title);
        TextView videoTimeText = convertView.findViewById(R.id.video_date);
        TextView videoReactionText = convertView.findViewById(R.id.video_reaction);
        ImageView videoThumbnail = convertView.findViewById(R.id.video_icon);

        videoTitleText.setText(videoTitle);
        videoTimeText.setText(videoTime);
        videoReactionText.setText(videoReaction);
        Glide.with(context)
                .load(videoThumb) // or URI/path
                .into(videoThumbnail);

        return convertView;
    }
}
