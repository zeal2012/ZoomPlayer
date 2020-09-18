package com.fei.smart.tech.zoom.player;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    String TAG = "MainActivity_";

//    VideoView videoView;

    private SurfaceView targetView;
    private View ll_progress;
    private MediaPlayer mediaPlayer;

//    private final String TEST_URL = "https://media.w3.org/2010/05/sintel/trailer.mp4";
    private final String TEST_URL = "https://v-cdn.zjol.com.cn/276984.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        ll_progress = findViewById(R.id.ll_progress);
        targetView = findViewById(R.id.view_target);
        targetView.getHolder().addCallback(this);

//        videoView = findViewById(R.id.videoview);
//        videoView.setMediaController(new MediaController(this));
//        videoView.setVideoPath(TEST_URL);
//        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                mp.start();
//            }
//        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            ll_progress.setVisibility(View.VISIBLE);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    ll_progress.setVisibility(View.GONE);
                    mediaPlayer.start();
                }
            });
            mediaPlayer.setDisplay(holder);
            mediaPlayer.setDataSource(TEST_URL);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

}