package com.qxueyou.qxueyouvideo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ijkplayer.IjkVideoView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private IjkVideoView mVideoPlayer;       //播放器
    private ImageButton mPlayAndStopBtn;    //播放和暂停按钮
    private ImageButton mFullScreenBtn;     //全屏按钮
    private TextView mCurrentTimeTv;        //视频当前时间
    private TextView mTotalTimeTv;          //视频总时长
    private SeekBar mProgressSb;            //视频播放进度
    private LinearLayout mLoadingLayout;    //加载显示控件
    private TextView mErrorTv;           //播放出错显示文本

    private int mCurrentPosition;       //视频当前播放时间点

    private static final int CHANGE_PLAY_TIME = 0x01;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CHANGE_PLAY_TIME) {
                //每秒修改显示时间和seekbar的进度
                mCurrentTimeTv.setText(formatTime(mVideoPlayer.getCurrentPosition()));
                mProgressSb.setProgress(mVideoPlayer.getCurrentPosition());

                //显示缓冲数据
                mProgressSb.setSecondaryProgress(mVideoPlayer.getBufferPercentage());
                mHandler.sendEmptyMessageDelayed(CHANGE_PLAY_TIME, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main_video);
        initView();
        initEvent();
        initData();
    }

    /**
     * 初始化界面控件
     */
    private void initView() {
        mVideoPlayer = (IjkVideoView) findViewById(R.id.videoview);
        mPlayAndStopBtn = (ImageButton) findViewById(R.id.play_btn);
        mFullScreenBtn = (ImageButton) findViewById(R.id.play_screen);
        mCurrentTimeTv = (TextView) findViewById(R.id.play_time);
        mTotalTimeTv = (TextView) findViewById(R.id.total_time);
        mProgressSb = (SeekBar) findViewById(R.id.seekbar);
        mLoadingLayout = (LinearLayout) findViewById(R.id.video_progress_layout);
        mErrorTv = (TextView) findViewById(R.id.play_error_text);
    }

    /**
     * 初始化事件
     */
    private void initEvent() {
        mPlayAndStopBtn.setOnClickListener(this);
        mFullScreenBtn.setOnClickListener(this);
        mProgressSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            //停止拖动后，将播放器设置到相应的播放时间
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mVideoPlayer.seekTo(seekBar.getProgress());
            }
        });
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //播放地址
        String url = "http://qxueyou-test.oss-cn-shenzhen.aliyuncs.com/video/297ebe0e5180b90d015185917ced1afc/ab8a6dac-f5a8-4830-b074-a4b5b64aaa07.mp4";
        mVideoPlayer.setVideoPath(url);
        mVideoPlayer.requestFocus();

        //播放器准备完成后的监听，准备完成后就可以，调用播放器的start方法，进行播放
        mVideoPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {

                mVideoPlayer.start();
                //设置视频的总时长
                mTotalTimeTv.setText(formatTime(mVideoPlayer.getDuration()));
                //发送消息每隔1秒更新显示时间
                mHandler.sendEmptyMessage(CHANGE_PLAY_TIME);
                mPlayAndStopBtn.setImageResource(R.mipmap.comment_play_mini);

                hindProgress();
                //将视频总时长设置为seekBar的最大值
                mProgressSb.setMax(mVideoPlayer.getDuration());
            }
        });

        //播放视频结束的监听
        mVideoPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {
                mPlayAndStopBtn.setImageResource(R.mipmap.video_play_little);
                Toast.makeText(MainActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
                mHandler.removeMessages(CHANGE_PLAY_TIME);
            }
        });

        //播放器出错了，显示“播放出错”
        mVideoPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                Log.e("onInfo", "---onError--i=" + i + "------i1=" + i1);
                //播放出错，显示错误文本
                mErrorTv.setVisibility(View.VISIBLE);
                mHandler.removeMessages(CHANGE_PLAY_TIME);
                hindProgress();
                return true;
            }
        });

        //播放器状态发生改变的监听
        mVideoPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {

                if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    //开始缓冲
                    showProgress();
                } else if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    //结束缓冲
                    hindProgress();
                }

                return false;
            }
        });
    }

    private void hindProgress() {
        mLoadingLayout.setVisibility(View.GONE);
    }

    private void showProgress() {
        mLoadingLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 设置控件点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        //播放暂停按钮
        if (v.getId() == R.id.play_btn) {
            if (mVideoPlayer.isPlaying()) {
                videoStop();
            } else {
                videoPlay();
            }
        }
        //全屏按钮
        if (v.getId() == R.id.play_screen) {
            Configuration mConfiguration1 = this.getResources().getConfiguration();//获取设置的配置信息
            int orientation1 = mConfiguration1.orientation;//获取屏幕方向
            if (orientation1 == Configuration.ORIENTATION_PORTRAIT) {
                //设置为横屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                //设置为竖屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    private void videoPlay() {
        mVideoPlayer.start();
        mPlayAndStopBtn.setImageResource(R.mipmap.comment_play_mini);
        mHandler.sendEmptyMessage(CHANGE_PLAY_TIME);
    }

    private void videoStop() {
        mVideoPlayer.pause();
        mPlayAndStopBtn.setImageResource(R.mipmap.video_play_little);
        mHandler.removeMessages(CHANGE_PLAY_TIME);
    }

    //适配时长时间显示格式
    private String formatTime(long timeTemp) {
        long timeData = checkTimeValue(timeTemp);
        timeData += 57600000;
        if (timeTemp >= 3600000) {   //大于一小时时出现小时
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
            return formatter.format(new Date(timeData));
        } else {
            DateFormat formatter = new SimpleDateFormat("mm:ss", Locale.CHINA);
            return formatter.format(new Date(timeData));
        }
    }

    private long checkTimeValue(long time) {
        if (time <= 0) {
            return 0;
        } else if (time > mVideoPlayer.getDuration()) {
            return mVideoPlayer.getDuration();
        } else {
            return time;
        }
    }

    /**
     * 横竖屏发生改变后调用
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mFullScreenBtn.setImageResource(R.mipmap.video_screen);
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mFullScreenBtn.setImageResource(R.mipmap.video_magnify);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoPlayer.isPlaying()) {
            mCurrentPosition = mVideoPlayer.getCurrentPosition();
            mVideoPlayer.pause();
            mHandler.removeMessages(CHANGE_PLAY_TIME);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mVideoPlayer.resume();
        mHandler.sendEmptyMessage(CHANGE_PLAY_TIME);
        mVideoPlayer.seekTo(mCurrentPosition);
    }
}
