package com.cosname.infiniteriddle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import android.widget.LinearLayout;

public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_PODIUM = 0;
    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_AD = 2;
    private static final int AD_POSITION = 3;
    private static final int ITEMS_PER_AD = 6;

    private List<ScoreEntry> scores;
    private String currentUsername = null;
    private static AdView singletonAdView = null;

    public LeaderboardAdapter(List<ScoreEntry> scores) {
        this.scores = scores;
    }
    public void setCurrentUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currentUsername = prefs.getString("username", null);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_PODIUM;
        if (position > 0 && (position - 1) % (ITEMS_PER_AD + 1) == ITEMS_PER_AD) return TYPE_AD;
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PODIUM) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_podium, parent, false);
            return new PodiumViewHolder(view);
        } else if (viewType == TYPE_AD) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_ad, parent, false);
            return new AdViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_normal, parent, false);
            return new NormalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PodiumViewHolder) {
            PodiumViewHolder podium = (PodiumViewHolder) holder;
            // Bind top 3
            if (scores.size() > 0) {
                ScoreEntry first = scores.get(0);
                podium.firstName.setText(formatName(first.name));
                podium.firstScore.setText(String.valueOf(first.score));
            } else {
                podium.firstName.setText("");
                podium.firstScore.setText("");
            }
            if (scores.size() > 1) {
                ScoreEntry second = scores.get(1);
                podium.secondName.setText(formatName(second.name));
                podium.secondScore.setText(String.valueOf(second.score));
            } else {
                podium.secondName.setText("");
                podium.secondScore.setText("");
            }
            if (scores.size() > 2) {
                ScoreEntry third = scores.get(2);
                podium.thirdName.setText(formatName(third.name));
                podium.thirdScore.setText(String.valueOf(third.score));
            } else {
                podium.thirdName.setText("");
                podium.thirdScore.setText("");
            }
        } else if (holder instanceof AdViewHolder) {
            AdViewHolder adHolder = (AdViewHolder) holder;
            Context context = adHolder.itemView.getContext();
            AdView adView = new AdView(context);
            adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
            adView.setAdUnitId("ca-app-pub-6979842665203661/1765118556");
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                }
                @Override
                public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError adError) {
                }
            });
            adView.loadAd(new AdRequest.Builder().build());
            ViewGroup parent = (ViewGroup) adView.getParent();
            if (parent != null) {
                parent.removeView(adView);
            }
            adHolder.container.removeAllViews();
            adHolder.container.addView(adView);
        } else if (holder instanceof NormalViewHolder) {
            int dataPos = getDataPosition(position);
            if (scores.size() > dataPos) {
                ScoreEntry entry = scores.get(dataPos);
                NormalViewHolder normal = (NormalViewHolder) holder;
                normal.positionText.setText(String.valueOf(calculatePosition(dataPos)));
                normal.nameText.setText(formatName(entry.name));
                normal.scoreText.setText(String.valueOf(entry.score));
            }
        }
    }

    private int getDataPosition(int adapterPosition) {
        if (adapterPosition == 0) return -1;
        int adsBefore = (adapterPosition - 1) / (ITEMS_PER_AD + 1);
        return adapterPosition - 1 - adsBefore + 3;
    }

    private int calculatePosition(int dataPos) {
        if (dataPos < 0 || dataPos >= scores.size()) {
            return dataPos + 1;
        }
        int position = dataPos + 1;
        int currentScore = scores.get(dataPos).score;
        for (int i = dataPos - 1; i >= 0; i--) {
            if (scores.get(i).score != currentScore) {
                position = i + 2;
                break;
            }
        }
        return position;
    }

    private String formatName(String name) {
        if (currentUsername != null && name.equals(currentUsername)) {
            return name + " (You)";
        }
        return name;
    }

    @Override
    public int getItemCount() {
        if (scores == null || scores.isEmpty()) return 0;
        int dataCount = scores.size();
        int normalRows = Math.max(0, dataCount - 3); // after podium
        int adRows = (normalRows + ITEMS_PER_AD - 1) / ITEMS_PER_AD; // one ad per ITEMS_PER_AD
        int count = 1; // Podium
        count += normalRows + adRows;
        return count;
    }

    static class PodiumViewHolder extends RecyclerView.ViewHolder {
        TextView firstName, firstScore, secondName, secondScore, thirdName, thirdScore;
        PodiumViewHolder(View itemView) {
            super(itemView);
            firstName = itemView.findViewById(R.id.firstName);
            firstScore = itemView.findViewById(R.id.firstScore);
            secondName = itemView.findViewById(R.id.secondName);
            secondScore = itemView.findViewById(R.id.secondScore);
            thirdName = itemView.findViewById(R.id.thirdName);
            thirdScore = itemView.findViewById(R.id.thirdScore);
        }
    }

    static class NormalViewHolder extends RecyclerView.ViewHolder {
        TextView positionText, nameText, scoreText;
        NormalViewHolder(View itemView) {
            super(itemView);
            positionText = itemView.findViewById(R.id.positionText);
            nameText = itemView.findViewById(R.id.nameText);
            scoreText = itemView.findViewById(R.id.scoreText);
        }
    }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        AdViewHolder(View itemView) {
            super(itemView);
            container = (LinearLayout) itemView;
        }
    }

}